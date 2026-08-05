# API 예외 처리

## 1. API 오류 응답은 HTML과 다르게 생각해야 한다

웹 화면에서는 오류가 발생했을 때 사용자가 볼 수 있는 오류 페이지를 보여주면 된다.

하지만 API는 화면이 아니라 데이터를 주고받기 때문에, 정상 응답뿐 아니라 오류 응답도 클라이언트가 처리할 수 있는 형태로 내려줘야 한다.

```text
화면 요청
→ 오류가 발생하면 HTML 오류 화면 반환

API 요청
→ 오류가 발생하면 JSON 형태의 오류 정보 반환
ex) @RequestMapping(value = "/error-page/500", produces = MediaType.APPLICATION_JSON_VALUE
```

따라서 API에서는 오류 상황마다 어떤 상태 코드와 데이터를 반환할지 정하는 것이 중요하다.

---

## 2. 기존 오류 페이지 방식의 문제점

API 컨트롤러가 정상적으로 실행되면 객체가 JSON으로 변환되어 반환된다.

그런데 컨트롤러에서 예외가 발생하고, 기존에 등록한 오류 페이지 방식으로 처리되면 API 요청임에도 HTML 오류 화면이 반환될 수 있다.

```text
정상 API 요청
→ JSON 응답

예외가 발생한 API 요청
→ HTML 오류 화면
```

API 클라이언트는 보통 JSON 응답을 기대하기 때문에, 오류가 발생했을 때 HTML이 내려오면 응답을 일관되게 처리하기 어렵다.

그래서 오류 처리 경로에서도 요청이 원하는 응답 형식에 맞춰 JSON을 반환할 수 있어야 한다.

---

## 3. Accept 헤더에 따라 오류 응답이 달라질 수 있다

클라이언트는 `Accept` 헤더를 통해 어떤 형식의 응답을 원하는지 서버에 전달한다.

```text
Accept: text/html
→ HTML 응답을 원함

Accept: application/json
→ JSON 응답을 원함
```

오류 처리 컨트롤러에서도 이 값을 기준으로 어떤 메서드를 실행할지 구분할 수 있다.

JSON 응답을 반환하는 메서드에서는 오류 상태와 메시지를 `Map` 등에 담고, `ResponseEntity`로 반환할 수 있다.

```text
오류 정보 생성
→ ResponseEntity의 Body에 담음
→ 메시지 컨버터가 JSON으로 변환
```

즉, 오류 페이지 처리 과정에서도 요청이 원하는 미디어 타입에 맞춰 응답 형식을 다르게 만들 수 있다.

---

## 4. Spring Boot의 기본 API 오류 처리

Spring Boot는 오류가 발생했을 때 사용할 공통 경로로 `/error`를 기본 제공한다.

이 경로는 `BasicErrorController`가 처리한다.

```text
오류 발생
→ WAS의 오류 처리
→ /error로 내부 전달
→ BasicErrorController 실행
```

`BasicErrorController`는 같은 `/error` 경로에서도 요청의 `Accept` 헤더에 따라 다른 응답을 반환한다.

```text
text/html 요청
→ 오류 View 반환

그 외 요청
→ 오류 정보를 HTTP Body에 담아 반환
```

따라서 API 클라이언트가 JSON을 요청하면 Spring Boot의 기본 오류 처리 기능이 JSON 형태의 오류 응답을 만들어 줄 수 있다.

오류 응답에는 다음과 같은 정보가 포함될 수 있다.

```text
오류 발생 시간
HTTP 상태 코드
오류 이름
오류 메시지
요청 경로
```

---

## 5. 처리되지 않은 예외는 기본적으로 500이 된다

컨트롤러에서 발생한 예외를 아무도 처리하지 못하면 예외는 `DispatcherServlet`을 지나 WAS까지 전달된다.

WAS는 서버 내부에서 요청 처리에 실패한 것으로 판단하고 보통 500 상태 코드를 반환한다.

```text
Controller에서 예외 발생
→ DispatcherServlet
→ 예외 처리 실패
→ WAS
→ 500 Internal Server Error
```

하지만 모든 예외를 500으로 처리하는 것이 항상 적절한 것은 아니다.

예를 들어 클라이언트가 잘못된 값을 보낸 상황이라면 서버 내부 오류보다는 잘못된 요청으로 보는 것이 더 자연스러울 수 있다.

이런 경우에는 예외 종류에 따라 상태 코드를 바꿔줄 방법이 필요하다.

---

## 6. HandlerExceptionResolver의 역할

Spring MVC는 컨트롤러 밖으로 전달된 예외를 처리하기 위해 `HandlerExceptionResolver`를 제공한다.

`HandlerExceptionResolver`는 컨트롤러에서 발생한 예외를 전달받아, 그 예외를 어떻게 처리할지 결정한다.

```text
Controller에서 예외 발생
→ DispatcherServlet로 예외 전달
→ HandlerExceptionResolver가 예외 해결 시도
```

Resolver에서는 다음과 같은 작업을 할 수 있다.

```text
응답 상태 코드 변경
오류 메시지 설정
오류 View 선택
다음 Resolver에게 처리 기회 넘기기
```

즉, 자바 예외를 그대로 WAS까지 보내는 대신 HTTP 응답에 맞게 바꾸는 역할을 한다.

---

## 7. 예외가 Resolver에서 해결되지 않은 경우

컨트롤러에서 발생한 예외를 Resolver가 해결하지 못하면 다음 흐름으로 진행된다.

```text
WAS
→ Filter
→ DispatcherServlet
→ Interceptor preHandle
→ Controller
→ 예외 발생
→ postHandle 호출되지 않음
→ 예외가 DispatcherServlet로 전달
→ afterCompletion 호출
→ 예외가 WAS까지 전달
→ 오류 처리 경로로 내부 전달
→ BasicErrorController 실행
```

컨트롤러가 정상적으로 반환하지 못했기 때문에 `postHandle()`은 호출되지 않는다.

반면 `afterCompletion()`은 요청이 마무리되는 시점에 호출되므로 예외가 발생한 경우에도 실행된다.

---

## 8. HandlerExceptionResolver가 예외를 처리하는 경우

`DispatcherServlet`은 컨트롤러에서 예외가 발생하면 등록된 Resolver들에게 처리를 요청한다.

```text
WAS
→ Filter
→ DispatcherServlet
→ Interceptor preHandle
→ Controller
→ 예외 발생
→ postHandle 호출되지 않음
→ DispatcherServlet
→ HandlerExceptionResolver가 예외 해결 시도
```

이후 흐름은 Resolver가 어떤 값을 반환하고, 응답을 어떻게 처리했는지에 따라 달라진다.

---

## 9. 빈 ModelAndView를 반환한 경우

Resolver가 빈 `ModelAndView`를 반환하면 `DispatcherServlet`은 해당 예외가 해결된 것으로 판단한다.

```text
빈 ModelAndView 반환
→ 예외가 해결된 것으로 판단
→ View 렌더링 없음
→ 원래 예외는 WAS까지 전달되지 않음
```


---

## 10. View 정보가 담긴 ModelAndView를 반환한 경우

Resolver가 View와 Model 정보를 담은 `ModelAndView`를 반환하면 해당 View를 렌더링한다.

```text
예외 발생
→ Resolver가 오류 View 지정
→ 지정된 View 렌더링
```

이 경우에는 Resolver가 직접 오류 화면을 선택한 것이므로, 기존 오류 처리 경로로 다시 이동하지 않는다.

---

## 11. null을 반환한 경우

Resolver가 `null`을 반환하면 현재 Resolver가 해당 예외를 처리하지 않았다는 뜻이다.

```text
현재 Resolver가 null 반환
→ 다음 Resolver가 예외 해결 시도
```

등록된 Resolver가 모두 `null`을 반환하면 예외는 해결되지 않은 상태로 다시 `DispatcherServlet` 밖으로 전달된다.

```text
모든 Resolver가 처리하지 못함
→ 예외가 WAS까지 전달
→ 오류 처리 경로 실행
```

---

## 12. sendError()와 빈 ModelAndView를 함께 사용한 경우

Resolver에서 `sendError()`를 호출하면 응답 객체에 오류 상태가 기록된다.

그리고 빈 `ModelAndView`를 반환하면 원래 예외는 해결된 것으로 처리된다.

```text
Resolver가 예외 확인
→ sendError()로 오류 상태 설정
→ 빈 ModelAndView 반환
→ 원래 예외는 해결된 것으로 판단
```

이후 요청이 WAS로 돌아가면 WAS는 응답에 기록된 오류 상태를 확인하고 오류 처리 절차를 실행한다.

```text
WAS가 오류 상태 확인
→ /error로 내부 전달
→ BasicErrorController 실행
→ JSON 또는 HTML 오류 응답 반환
```

이때 원래 예외가 다시 WAS로 전달된 것은 아니다.

```text
원래 예외
→ Resolver가 해결

오류 처리 재실행
→ sendError()가 발생시킴
```

즉, 예외 해결과 WAS의 오류 처리 실행은 서로 구분해서 이해해야 한다.

## 13. Resolver에서 응답까지 만들면 예외 처리를 Spring MVC 안에서 끝낼 수 있다

컨트롤러에서 발생한 예외가 처리되지 않으면 WAS까지 전달되고, 이후 `/error`가 다시 호출된다.

`HandlerExceptionResolver`가 예외에 맞는 응답을 직접 만들면 원래 예외를 WAS까지 올리지 않고 요청을 마무리할 수 있다.

```text
예외를 처리하지 못한 경우
Controller
→ DispatcherServlet
→ WAS
→ /error
→ 오류 응답
```

```text
Resolver에서 처리한 경우
Controller
→ DispatcherServlet
→ HandlerExceptionResolver
→ 오류 응답 작성
→ 요청 종료
```

따라서 Resolver의 역할은 예외를 단순히 잡는 것에서 끝나지 않는다.

예외를 HTTP 상태 코드와 응답 데이터 또는 오류 화면으로 바꿔서 정상적인 응답 흐름으로 마무리한다.

---

## 14. Resolver는 자신이 담당하는 예외만 처리한다

Resolver에는 현재 발생한 예외 객체가 전달된다.

이 예외가 자신이 처리할 대상인지 확인하고, 대상이라면 응답을 만든다. 처리 대상이 아니면 `null`을 반환해서 다른 Resolver가 처리할 수 있도록 넘긴다.

```text
담당하는 예외
→ 상태 코드와 응답 구성
→ ModelAndView 반환

담당하지 않는 예외
→ null 반환
→ 다른 Resolver가 처리 시도
```

예제의 `UserHandlerExceptionResolver`는 `UserException`만 처리하고, 나머지 예외는 처리하지 않는다.

직접 만든 Resolver는 `WebConfig`의 `extendHandlerExceptionResolvers()`에 등록해야 실제 예외 처리 과정에 참여한다.

---

## 15. 같은 예외라도 Accept에 따라 응답 형식을 다르게 만들 수 있다

API 요청과 화면 요청은 원하는 오류 응답 형식이 다를 수 있다.

Resolver에서도 요청의 `Accept` 헤더를 확인해서 JSON 응답과 HTML 오류 화면을 구분할 수 있다.

```text
Accept: application/json
→ JSON 오류 데이터 반환

그 외
→ 오류 View 렌더링
```

JSON 응답을 만들 때는 예외 종류와 메시지 같은 정보를 `Map`에 담고, `ObjectMapper`로 JSON 형식의 문자열로 변환한다.

```text
예외 정보
→ Map에 저장
→ ObjectMapper로 JSON 문자열 변환
→ 응답 메시지 바디에 작성
```

이 과정에서는 응답 상태 코드를 `400 Bad Request`로 설정하고, `Content-Type`도 `application/json`으로 지정한다.

---

## 16. 응답을 직접 작성한 경우에는 빈 ModelAndView를 반환한다

Resolver에서 `response.getWriter().write()`로 응답 메시지 바디를 직접 작성했다면 추가로 렌더링할 View가 필요하지 않다.

이때 빈 `ModelAndView`를 반환하면 `DispatcherServlet`은 예외가 해결됐다고 판단한다.

```text
응답 상태 코드 설정
→ JSON 응답 바디 작성
→ 빈 ModelAndView 반환
→ 예외 처리 완료
```

반대로 HTML 오류 화면을 보여주려면 View 이름이 담긴 `ModelAndView`를 반환하면 된다.

```text
빈 ModelAndView
→ View 렌더링 없음

View가 있는 ModelAndView
→ 지정한 오류 View 렌더링
```

두 경우 모두 `ModelAndView`를 반환했으므로 원래 예외는 WAS까지 전달되지 않는다.

---

## 17. 직접 Resolver를 구현하면 예외 응답 과정을 모두 작성해야 한다

직접 만든 `HandlerExceptionResolver`에서는 예외 처리에 필요한 내용을 하나씩 작성해야 한다.

```text
처리할 예외 확인
응답 상태 코드 설정
Accept 헤더 확인
오류 데이터 생성
JSON 문자열 변환
응답 메시지 바디 작성
ModelAndView 반환
```

원하는 방식으로 예외를 처리할 수 있다는 장점은 있지만, 처리해야 할 과정이 많아 구현이 복잡해진다.

그래서 스프링은 자주 사용하는 예외 처리 기능을 기본 Resolver로 제공한다.

---

## 18. @ResponseStatus는 예외와 HTTP 상태 코드를 연결한다

예외 클래스에 `@ResponseStatus`를 붙이면 해당 예외가 발생했을 때 사용할 HTTP 상태 코드와 메시지를 정할 수 있다.

```java
@ResponseStatus(
    code = HttpStatus.BAD_REQUEST,
    reason = "잘못된 요청 오류"
)
public class BadRequestException extends RuntimeException {
}
```

`BadRequestException`이 컨트롤러 밖으로 전달되면 `ResponseStatusExceptionResolver`가 애노테이션을 확인한다.

```text
BadRequestException 발생
→ @ResponseStatus 확인
→ 응답 상태를 400 Bad Request로 변경
→ reason을 오류 메시지로 사용
```

예외가 원래 500으로 처리될 상황이어도, 애노테이션에 지정된 상태 코드가 적용된다.

---

## 19. ResponseStatusExceptionResolver는 내부적으로 sendError()를 호출한다

`ResponseStatusExceptionResolver`는 상태 코드와 `reason`을 적용할 때 `response.sendError()`를 사용한다.

```text
@ResponseStatus 정보 확인
→ sendError(상태 코드, reason)
→ 빈 ModelAndView 반환
```

빈 `ModelAndView`를 반환했으므로 원래 예외는 Resolver에서 해결된 것으로 처리된다.

하지만 `sendError()`가 호출됐기 때문에 WAS는 오류 상태를 확인하고 `/error` 처리 과정을 실행한다.

```text
원래 예외
→ Resolver에서 해결

sendError()
→ WAS가 오류 상태 확인
→ /error로 내부 전달
→ 오류 응답 생성
```

즉, 원래 예외가 처리되지 않아서 WAS까지 다시 전달된 것이 아니다. 예외는 Resolver에서 끝났고, `sendError()`가 WAS의 오류 처리 흐름을 시작시킨 것이다.

---

## 20. reason에는 MessageSource의 메시지 코드를 사용할 수 있다

`@ResponseStatus`의 `reason`에는 오류 문장을 직접 적을 수도 있고, 메시지 코드를 지정할 수도 있다.

```java
@ResponseStatus(
    code = HttpStatus.BAD_REQUEST,
    reason = "error.bad"
)
```

```properties
error.bad=잘못된 요청 오류입니다. 메시지 사용
```

`reason`에 메시지 코드를 넣으면 `ResponseStatusExceptionResolver`가 `MessageSource`에서 실제 메시지를 조회한다.

```text
reason에 메시지 코드 지정
→ MessageSource에서 메시지 조회
→ 조회한 문장을 오류 메시지로 사용
```

---

## 21. ResponseStatusException은 예외를 발생시키는 시점에 상태 코드를 정한다

`@ResponseStatus`는 예외 클래스에 직접 선언해야 한다.

따라서 코드를 수정할 수 없는 외부 예외에는 적용할 수 없고, 애노테이션 값이 고정되어 있어 상황마다 상태 코드를 다르게 주기도 어렵다.

이때는 `ResponseStatusException`을 직접 발생시키면 된다.

```java
throw new ResponseStatusException(
    HttpStatus.NOT_FOUND,
    "error.bad",
    new IllegalArgumentException()
);
```

`ResponseStatusException`에는 HTTP 상태 코드, 메시지, 원인이 된 예외를 함께 담을 수 있다.

```text
ResponseStatusException 발생
→ ResponseStatusExceptionResolver가 처리
→ 지정한 상태 코드와 메시지 적용
```












