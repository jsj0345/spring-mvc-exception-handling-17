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

`@ResponseStatus`나 `ResponseStatusException`에서 결국 `sendError()`를 거치기 때문에 Resolver 안에서 완전히 끝나는 흐름은 아님.

다음 코드를 참고하자. 
```java
protected ModelAndView applyStatusAndReason(int statusCode, @Nullable String reason, HttpServletResponse response) throws IOException {
    if (!StringUtils.hasLength(reason)) {
      response.sendError(statusCode);
    } else {
      String resolvedReason = this.messageSource != null ? this.messageSource.getMessage(reason, (Object[])null, reason, LocaleContextHolder.getLocale()) : reason;
      response.sendError(statusCode, resolvedReason);
    }

    return new ModelAndView();
  }
```

## 22. 타입 변환 오류도 Resolver가 HTTP 응답에 맞게 정리한다

컨트롤러 메서드의 파라미터 타입과 실제 요청 값이 맞지 않으면 컨트롤러 메서드가 실행되기 전에 변환 과정에서 예외가 발생할 수 있다.

예를 들어 다음 요청을 생각해보자.

```text
/api/default-handler-ex?data=aaa
```

컨트롤러는 `data`를 `Integer`로 받도록 선언되어 있다.

```java
@GetMapping("/api/default-handler-ex")
public String defaultException(@RequestParam("data") Integer data) {
    return "ok";
}
```

`aaa`는 `Integer`로 바꿀 수 없기 때문에 정상적으로 파라미터를 만들 수 없다.

이런 종류의 예외까지 전부 500으로 내려버리면 서버 내부 로직이 고장 난 것처럼 보인다. 하지만 실제 원인은 클라이언트가 숫자가 필요한 자리에 문자를 보낸 것이다.

Spring MVC는 이런 상황을 기본 Resolver 중 하나인 `DefaultHandlerExceptionResolver`에서 다룬다.

```text
요청 값 변환 실패
→ 타입 관련 예외 발생
→ DispatcherServlet이 Resolver들에게 처리 요청
→ DefaultHandlerExceptionResolver가 처리
→ 응답 상태를 400으로 변경
```

즉, 자바 예외의 종류를 보고 HTTP 관점에서 더 알맞은 상태 코드로 바꾸는 역할을 한다고 이해하면 된다.

---

## 23. DefaultHandlerExceptionResolver도 sendError()를 사용하는 경우가 있다

타입 변환 실패를 처리하는 내부 로직을 보면 핵심은 다음 정도다.

```java
response.sendError(400);
return new ModelAndView();
```

여기서 두 동작을 분리해서 봐야 한다.

```text
빈 ModelAndView 반환
→ 발생했던 예외는 Resolver가 처리한 것으로 판단

sendError(400)
→ 응답에 오류 상태 기록
→ WAS의 오류 처리 절차가 다시 동작할 수 있음
```

따라서 `DefaultHandlerExceptionResolver`가 예외를 처리하지 못해서 `/error`로 가는 것이 아니다.

**예외 자체는 Resolver가 정리했지만, 처리 과정에서 `sendError(400)`을 호출했기 때문에 WAS의 오류 처리 흐름이 이어지는 것**이다.

---

## 24. 그래서 최종 응답에 BasicErrorController의 오류 정보가 보일 수 있다

현재 프로젝트에서 다음과 같이 잘못된 값을 요청하면 400 응답이 내려온다.

```text
/api/default-handler-ex?data=qqq
```

응답은 대략 다음과 같은 형태가 된다.

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "path": "/api/default-handler-ex"
}
```

이 결과를 보고 `DefaultHandlerExceptionResolver`가 JSON을 직접 만든다고 생각하면 안 된다.

흐름은 다음과 같이 나눠서 보는 편이 정확하다.

```text
타입 변환 실패
→ DefaultHandlerExceptionResolver
→ sendError(400)
→ 빈 ModelAndView 반환
→ 원래 예외는 해결됨
→ WAS 오류 처리
→ /error
→ BasicErrorController
→ 오류 정보를 Body에 담아 응답
```

`timestamp`, `status`, `error`, `path` 같은 공통 오류 정보가 보이는 이유도 마지막에 Spring Boot의 기본 오류 처리 경로를 거쳤기 때문이다.

---

## 25. 상태 코드를 정리하는 것과 API 오류 응답을 설계하는 것은 별개다

여기까지의 Resolver를 보면서 한 가지 구분할 필요가 있다.

예외를 적절한 HTTP 상태 코드로 바꾸는 것과, 클라이언트가 실제로 사용할 오류 데이터를 만드는 것은 같은 일이 아니다.

예를 들어 `DefaultHandlerExceptionResolver`는 타입 변환 실패를 400으로 정리해준다.

```text
문자열을 Integer로 변환 실패
→ 예외 발생
→ DefaultHandlerExceptionResolver
→ 400 Bad Request
```

HTTP 관점에서는 이것만으로도 잘못된 요청이라는 의미가 전달된다.

하지만 프론트나 다른 서버가 이 응답을 받아서 실제 처리를 해야 한다면 상태 코드만으로는 부족할 수 있다.

```json
{
  "code": "INVALID_PARAMETER",
  "message": "data는 숫자로 입력해야 합니다."
}
```

이처럼 API에서는 상태 코드 외에도 클라이언트가 판단에 사용할 오류 코드나 메시지처럼 **우리 서비스가 정한 응답 구조**가 필요할 수 있다.

Spring Boot의 기본 오류 응답을 그대로 사용할 수도 있지만, 모든 API가 항상 같은 오류 정보만 필요한 것은 아니다.

또 앞에서 직접 만든 `HandlerExceptionResolver`처럼 원하는 JSON을 직접 작성할 수도 있다.
다만 그렇게 하면 다음 작업까지 예외 처리 코드에서 직접 책임져야 했다.

```text
상태 코드 지정
→ Content-Type 지정
→ 객체를 JSON 문자열로 변환
→ HttpServletResponse Body에 직접 기록
```

이 방식도 동작은 하지만 일반 컨트롤러에서는 객체 하나만 반환해도 메시지 컨버터가 응답을 만들어주던 것과 비교하면 작성해야 할 코드가 많다.

그래서 다음 단계에서는 **예외 처리에서도 일반 컨트롤러처럼 객체를 반환하고, Spring MVC의 응답 처리 기능을 그대로 활용하는 방법**을 사용한다.

---

## 26. 예외 처리도 컨트롤러 메서드처럼 분리할 수 있다

앞에서는 `HandlerExceptionResolver`를 직접 구현해서 예외를 처리했다.

이 방식에서는 예외 종류를 직접 확인한 뒤, 필요한 경우 상태 코드와 `Content-Type`을 지정하고 객체를 JSON으로 변환한 다음 `HttpServletResponse`의 Body에 직접 기록하는 과정까지 코드로 작성해야 했다.

반면 `@ExceptionHandler`를 사용하면 컨트롤러에서 발생한 특정 예외를 별도의 예외 처리 메서드로 연결할 수 있다.

```java
@ExceptionHandler(IllegalArgumentException.class)
public ErrorResult illegalExHandler(IllegalArgumentException e) {
    return new ErrorResult("BAD", e.getMessage());
}
```

해당 컨트롤러에서 `IllegalArgumentException`이 발생하면 `ExceptionHandlerExceptionResolver`가 현재 예외를 처리할 수 있는 `@ExceptionHandler` 메서드를 찾고, 일치하는 메서드를 실행한다.

```text
Controller에서 IllegalArgumentException 발생
→ DispatcherServlet
→ ExceptionHandlerExceptionResolver
→ 처리 가능한 @ExceptionHandler 메서드 탐색
→ illegalExHandler() 실행
```

즉, 직접 `HandlerExceptionResolver`를 구현하는 방식처럼 예외 분기와 응답 처리 과정을 하나의 Resolver 안에서 수동으로 작성하는 대신, 예외별 처리 로직을 일반 컨트롤러 메서드처럼 분리해서 작성할 수 있다.

단, `@ExceptionHandler` 자체가 반환 객체를 JSON으로 변환하는 것은 아니다. 반환값이 실제 HTTP Body에 어떻게 들어가는지는 `@RestController`, `@ResponseBody`, `ResponseEntity`와 같은 응답 처리 방식에 따라 결정되며, 이 부분은 다음에서 이어서 정리한다.

---

## 27. @ExceptionHandler 자체가 JSON을 만드는 것은 아니다

`@ExceptionHandler`의 역할은 **어떤 예외를 어떤 메서드가 담당할지 연결하는 것**이다.

반환 객체를 JSON으로 만드는 기능까지 `@ExceptionHandler`가 담당하는 것은 아니다.

현재 `ApiExceptionV2Controller`는 `@RestController`이므로 반환값이 응답 Body로 처리된다.

```java
@RestController
public class ApiExceptionV2Controller {

    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult illegalExHandler(IllegalArgumentException e) {
        return new ErrorResult("BAD", e.getMessage());
    }
}
```

흐름은 다음과 같다.

```text
@ExceptionHandler
→ 처리할 예외와 메서드를 연결

@RestController / @ResponseBody
→ 반환 객체를 HTTP Body로 처리

HttpMessageConverter
→ ErrorResult 객체를 JSON으로 변환
```

따라서 `@ExceptionHandler = JSON 변환`으로 외우면 안 된다.

---

## 28. 예외를 처리한 뒤에는 상태 코드도 따로 생각해야 한다

예외 처리 메서드가 정상적으로 실행되어 `ErrorResult` 객체를 반환했다고 해서 HTTP 상태 코드까지 자동으로 400이나 500이 되는 것은 아니다.

예를 들어 다음 코드에서 `@ResponseStatus`를 빼면 예외는 처리됐지만 별도의 상태 코드를 지정하지 않았기 때문에 정상 응답 상태인 200으로 끝날 수 있다.

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
@ExceptionHandler(IllegalArgumentException.class)
public ErrorResult illegalExHandler(IllegalArgumentException e) {
    return new ErrorResult("BAD", e.getMessage());
}
```

따라서 API 오류 응답에서는 두 가지를 같이 봐야 한다.

```text
1. Body에는 어떤 오류 데이터를 보낼 것인가?
2. HTTP 상태 코드는 무엇으로 보낼 것인가?
```

현재 예제에서는 `@ResponseStatus(HttpStatus.BAD_REQUEST)`를 사용해 400을 지정한다.

---

## 29. ResponseEntity를 반환하면 상태 코드와 Body를 한 번에 정할 수 있다

`@ResponseStatus` 대신 `ResponseEntity`를 반환해도 된다.

```java
@ExceptionHandler(UserException.class)
public ResponseEntity<ErrorResult> userExHandler(UserException e) {
    ErrorResult body = new ErrorResult("USER-EX", e.getMessage());
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
}
```

`ResponseEntity`는 응답의 주요 요소를 하나의 객체로 표현한다.

```text
ResponseEntity
├─ Body   : ErrorResult
├─ Header : 필요한 경우 추가 가능
└─ Status : 400 Bad Request
```

`@RestController`가 없더라도 `ResponseEntity` 반환 타입은 Spring MVC가 HTTP 응답으로 직접 처리한다.

다만 `ResponseEntity`가 스스로 JSON 문자열을 만드는 것은 아니다.

```text
ResponseEntity 반환
→ Spring MVC가 상태 코드와 헤더를 HttpServletResponse에 반영
→ Body 객체는 HttpMessageConverter에 전달
→ Jackson 컨버터가 선택되면 JSON으로 직렬화
→ 응답 Body에 기록
```

즉, 개발자가 `HttpServletResponse`에 직접 `write()` 하는 것과 결과는 비슷해 보여도 처리 방식은 다르다. 실제 응답 객체에 값을 기록하는 작업은 Spring MVC가 대신한다.

---

## 30. @ExceptionHandler에는 예외 타입을 생략할 수도 있다

처리할 예외는 애노테이션에 직접 작성할 수도 있다.

```java
@ExceptionHandler(UserException.class)
```

또는 메서드 파라미터의 예외 타입을 기준으로 추론하게 둘 수도 있다.

```java
@ExceptionHandler
public ResponseEntity<ErrorResult> userExHandler(UserException e) {
    // ...
}
```

현재 예제처럼 파라미터 타입이 명확하다면 둘 다 `UserException`을 처리하는 메서드로 사용할 수 있다.

학습할 때는 다음처럼 이해하면 충분하다.

```text
@ExceptionHandler에 예외 타입 명시
→ 그 타입을 기준으로 매칭

예외 타입 생략
→ 메서드 파라미터의 예외 타입을 기준으로 판단
```

---

## 31. 부모 예외와 자식 예외가 함께 걸리면 더 구체적인 쪽이 선택된다

예외도 상속 관계가 있기 때문에 부모 예외를 지정하면 그 하위 예외도 처리 대상이 될 수 있다.

예를 들어 다음 두 메서드가 있다고 생각해보자.

```text
IllegalArgumentException 처리 메서드
Exception 처리 메서드
```

`IllegalArgumentException`은 `Exception`의 하위 타입이므로 두 메서드 모두 후보가 될 수 있다.

이 경우에는 실제 발생한 예외와 더 가까운 `IllegalArgumentException` 처리 메서드가 우선된다.

```text
IllegalArgumentException 발생
→ IllegalArgumentException용 Handler 후보
→ Exception용 Handler도 후보
→ 더 구체적인 IllegalArgumentException Handler 선택
```

현재 컨트롤러의 마지막 `Exception` 처리 메서드는 앞에서 잡지 못한 예외를 처리하는 공통 안전망처럼 볼 수 있다.

```java
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
@ExceptionHandler
public ErrorResult exHandler(Exception e) {
    return new ErrorResult("EX", "내부 오류");
}
```

구체적인 예외는 앞의 메서드에서 처리하고, 나머지 예상하지 못한 예외는 여기서 500으로 정리하는 구조다.

---

## 32. @ExceptionHandler는 기본적으로 해당 컨트롤러 안의 예외를 처리한다

컨트롤러 내부에 선언한 `@ExceptionHandler`는 그 컨트롤러에서 발생한 예외를 처리하는 데 적합하다.

이 방식의 장점은 같은 예외 타입이라도 컨트롤러의 역할에 맞춰 다른 응답을 만들기 쉽다는 점이다.

```text
회원 컨트롤러의 RuntimeException
→ 회원 API 규격에 맞는 오류 응답

주문 컨트롤러의 RuntimeException
→ 주문 API 규격에 맞는 오류 응답
```

직접 만든 전역 `HandlerExceptionResolver` 하나에서 모든 URI와 예외를 구분하는 것보다 책임을 나누기 편하다.

반대로 여러 컨트롤러에서 같은 예외 처리 코드를 반복하게 되면 중복이 생길 수 있다. 이런 공통 처리 문제는 이후 `@ControllerAdvice` 또는 `@RestControllerAdvice`로 분리할 수 있다.

---

## 33. 지금까지의 API 예외 처리 방식을 비교하면 차이가 명확하다

지금까지 사용한 방법을 한 번에 비교하면 다음과 같다.

```text
BasicErrorController
→ Spring Boot가 제공하는 공통 오류 응답 사용
→ 간단하지만 API별 세부 규격을 만들기에는 제한이 있음

직접 HandlerExceptionResolver 구현
→ 예외 처리 과정을 원하는 대로 제어 가능
→ HttpServletResponse, JSON 변환 등을 직접 다뤄야 해서 코드가 많아짐

@ExceptionHandler
→ 컨트롤러 메서드를 작성하듯 예외 처리 가능
→ 객체 반환과 메시지 컨버터를 그대로 활용할 수 있음
→ API별 오류 Body와 상태 코드를 만들기 편함
```

결국 API 예외 처리에서는 단순히 "예외를 없애는 것"보다 **예외를 클라이언트가 이해할 수 있는 HTTP 상태 코드와 오류 데이터로 바꾸는 것**이 핵심이다.
















