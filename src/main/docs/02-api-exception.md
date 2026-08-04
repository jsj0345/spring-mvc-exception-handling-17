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



