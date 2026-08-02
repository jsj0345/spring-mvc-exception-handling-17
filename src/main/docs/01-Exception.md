# 서블릿 예외 처리와 오류 화면

## 1. 처리되지 않은 예외는 어디까지 전달되는가

컨트롤러나 서비스에서 발생한 예외를 애플리케이션 내부에서 처리하지 못하면, 예외는 호출 경로를 거슬러 올라가 최종적으로 WAS까지 전달된다.

```text
Controller에서 예외 발생
← Interceptor
← DispatcherServlet
← Filter
← WAS
```

WAS는 이 요청을 정상적으로 처리할 수 없다고 판단하고 일반적으로 500 상태 코드를 반환한다.

반면 요청한 URL 자체를 처리할 대상이 없다면 404 상태 코드가 반환된다.

```text
404 → 요청한 대상을 찾지 못함
500 → 서버가 요청을 처리하던 중 오류 발생
```

---

## 2. `sendError()`로 오류 상태 전달하기

예외를 던지지 않고도 `HttpServletResponse`의 `sendError()`를 사용해 오류 상태를 전달할 수 있다.

```java
response.sendError(404);
```

`sendError()`는 새로운 예외를 발생시키는 코드가 아니다. 응답 객체에 오류 상태를 기록하고, 요청 처리가 WAS로 돌아갔을 때 WAS가 그 상태를 확인해 오류 응답을 만든다.

```text
컨트롤러에서 sendError() 호출
→ response에 오류 상태 기록
→ WAS가 상태 확인
→ 해당 상태 코드로 오류 응답 처리
```

즉, 처리되지 않은 예외와 `sendError()`는 시작 방식은 다르지만 최종 오류 응답은 WAS가 처리한다는 공통점이 있다.

---

## 3. 오류 종류와 처리 경로 연결하기

기본 오류 화면 대신 별도의 화면을 보여주려면 오류 종류와 처리 URL을 연결해야 한다.

```java
new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/404");
new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");
new ErrorPage(RuntimeException.class, "/error-page/500");
```

위 설정은 다음 의미를 가진다.

```text
404 상태 코드      → /error-page/404
500 상태 코드      → /error-page/500
RuntimeException   → /error-page/500
```

예외 타입으로 등록하면 해당 예외뿐 아니라 그 하위 타입도 같은 경로에서 처리된다.

---

## 4. `ErrorPage`의 URL은 화면 파일 경로가 아니다

`ErrorPage`에 작성한 URL은 HTML 파일 위치가 아니라, 오류가 발생했을 때 WAS가 다시 전달할 요청 경로다.

```text
오류 발생
→ WAS가 등록된 오류 경로로 다시 전달
→ 해당 URL의 컨트롤러 실행
→ 컨트롤러가 오류 뷰 이름 반환
→ 화면 렌더링
```

예를 들어 404 오류를 `/error-page/404`에 연결했다면, 해당 URL을 처리하는 컨트롤러가 필요하다.

```java
@RequestMapping("/error-page/404")
public String errorPage404() {
    return "error-page/404";
}
```

두 값은 역할이 다르다.

```text
/error-page/404 → WAS가 다시 요청하는 URL
error-page/404  → 컨트롤러가 반환하는 뷰 이름
```

따라서 `ErrorPage`는 오류 화면 파일을 직접 지정하는 설정이 아니라, 오류를 처리할 요청 경로를 연결하는 설정이다.

## 5. 오류 화면을 보여주기 위해 서버 내부에서 다시 요청한다

컨트롤러에서 처리되지 않은 예외가 발생하거나 `sendError()`가 호출되면 요청은 WAS로 돌아간다.

WAS는 등록된 `ErrorPage` 정보를 확인한 뒤, 해당 오류와 연결된 URL로 서버 내부 요청을 다시 보낸다.

```text
첫 번째 처리

클라이언트 요청
→ Filter
→ DispatcherServlet
→ Interceptor
→ Controller
→ 예외가 WAS까지 전달됨
```

```text
오류 화면 처리

WAS
→ 등록된 오류 처리 URL로 내부 전달
→ Filter
→ DispatcherServlet
→ Interceptor
→ 오류 처리 Controller
→ 오류 View
```

예를 들어 `RuntimeException`을 `/error-page/500`에 연결했다면, 예외가 WAS까지 전달된 뒤 `/error-page/500`을 처리하는 컨트롤러가 다시 실행된다.

이 과정은 브라우저가 오류 처리 URL을 새로 요청한 것이 아니다. 브라우저는 처음 보낸 요청에 대한 최종 응답만 받으며, 오류 화면을 찾는 과정은 서버 내부에서 진행된다.

---

## 6. 오류 처리 요청에는 발생한 오류 정보가 함께 전달된다

WAS는 오류 처리 URL만 다시 호출하는 것이 아니라, 처음 요청에서 발생한 오류 정보도 `request` 속성에 담아 전달한다.

오류 페이지에서는 필요에 따라 다음 정보를 확인할 수 있다.

```text
발생한 예외
예외 타입
오류 메시지
처음 요청한 URI
오류가 발생한 서블릿 이름
HTTP 상태 코드
```

서블릿에서는 이러한 값에 접근할 수 있도록 `RequestDispatcher`에 관련 상수를 제공한다.

```java
request.getAttribute(
        RequestDispatcher.ERROR_STATUS_CODE
);
```

모든 값을 화면에 노출해야 하는 것은 아니다. 이 정보는 오류 원인을 기록하거나, 상태 코드에 따라 화면을 구분하는 데 사용할 수 있다.

---

## 7. 오류 처리 URL에 직접 접근한 경우는 다르게 동작한다

브라우저에서 `/error-page/404` 같은 오류 처리 URL을 직접 입력할 수도 있다.

이 경우에는 실제 오류가 발생한 것이 아니라 일반 요청으로 해당 컨트롤러에 접근한 것이다. 따라서 WAS가 넣어 주는 예외 정보나 원래 요청 URI가 존재하지 않는다.

```text
실제 오류를 통해 이동

/error-ex 요청
→ 예외 발생
→ WAS가 /error-page/500으로 내부 전달
→ 오류 정보 존재
```

```text
오류 처리 URL에 직접 접근

/error-page/500 요청
→ 일반 컨트롤러 요청
→ 전달된 오류 정보 없음
```

같은 컨트롤러가 실행되더라도, 서버의 오류 처리 과정에서 호출됐는지 브라우저가 직접 호출했는지에 따라 요청에 담긴 정보가 달라진다.

---

## 8. `DispatcherType`으로 요청이 시작된 이유를 구분한다

오류 화면을 처리할 때는 서버 내부에서 요청 흐름이 한 번 더 실행된다. 그래서 필터와 DispatcherServlet도 다시 거칠 수 있다.

서블릿은 현재 요청이 어떤 이유로 전달됐는지 구분할 수 있도록 `DispatcherType`을 제공한다.

이번 범위에서 중요한 값은 다음 두 가지다.

```text
REQUEST
→ 브라우저에서 처음 들어온 일반 요청

ERROR
→ 오류 화면을 처리하기 위해 WAS가 내부 전달한 요청
```

현재 요청 종류는 다음과 같이 확인할 수 있다.

```java
request.getDispatcherType();
```

예외가 발생한 최초 요청에서는 `REQUEST`가 확인되고, WAS가 오류 처리 URL로 다시 전달한 요청에서는 `ERROR`가 확인된다.

```text
/error-ex
→ DispatcherType.REQUEST
→ 예외 발생

/error-page/500
→ DispatcherType.ERROR
→ 오류 화면 처리
```

---

## 9. 필터가 오류 처리 요청에도 동작할지 선택할 수 있다

오류 처리 요청도 필터 체인을 다시 지날 수 있지만, 모든 필터를 다시 실행해야 하는 것은 아니다.

예를 들어 요청 로그를 남기는 필터라면 일반 요청과 오류 요청을 모두 확인할 수 있다. 반면 이미 인증 검사를 마친 요청이라면 오류 화면을 보여주기 위해 인증 필터를 다시 실행할 필요가 없을 수 있다.

필터를 등록할 때 적용할 `DispatcherType`을 지정할 수 있다.

```java
registration.setDispatcherTypes(
        DispatcherType.REQUEST,
        DispatcherType.ERROR
);
```

위와 같이 등록하면 필터가 다음 두 요청에 모두 실행된다.

```text
일반 요청
→ REQUEST 필터 실행

오류 화면 내부 요청
→ ERROR 필터 실행
```

`REQUEST`만 지정하면 브라우저에서 처음 들어온 요청에만 필터가 적용되고, 오류 화면을 위한 내부 요청에서는 해당 필터가 실행되지 않는다.

따라서 필터의 역할을 보고 오류 처리 과정에서도 다시 실행해야 하는지 결정하면 된다.




