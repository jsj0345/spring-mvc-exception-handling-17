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


