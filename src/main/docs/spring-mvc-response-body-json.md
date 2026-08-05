# `@ResponseBody`, `@RestController`, `ResponseEntity`와 JSON 응답

아래 내용은 헷갈린 부분을 복습하기 위해 작성한 것이다. 

## 1. `@ResponseBody`와 `@RestController`의 역할

`@ResponseBody`와 `@RestController`는 컨트롤러에서 반환한 값을 논리적인 View 이름으로 처리하지 않고, HTTP 응답 메시지 바디에 직접 작성하도록 한다.

```java
@GetMapping("/user")
@ResponseBody
public User user() {
    return new User("kim", 20);
}
```

위 코드에서 `User` 객체 자체를 네트워크로 보내는 것은 아니다.

스프링이 `HttpMessageConverter`를 이용하여 반환값을 응답 메시지 바디에 작성한다.

객체를 반환했고 JSON 변환이 가능한 상황이라면, 일반적으로 Jackson을 사용하는 `MappingJackson2HttpMessageConverter`가 객체를 JSON 형식의 데이터로 변환한다.

```text
User 객체
→ HttpMessageConverter
→ Jackson
→ JSON 형식의 텍스트
→ HTTP 응답 메시지 바디
```

`@RestController`는 다음 두 애노테이션을 합친 것이라고 생각하면 된다.

```java
@Controller
@ResponseBody
```

따라서 `@RestController`가 붙은 컨트롤러에서는 메서드의 반환값이 기본적으로 응답 메시지 바디에 작성된다.

---

## 2. 객체를 반환하면 무조건 JSON이 되는 것은 아님

`@ResponseBody`와 `@RestController`의 정확한 역할은 반환값을 JSON으로 만드는 것이 아니라, 반환값을 HTTP 응답 메시지 바디에 작성하도록 하는 것이다.

그 반환값을 실제로 어떤 형식으로 변환할지는 `HttpMessageConverter`가 결정한다.

예를 들어 객체를 반환하면 보통 JSON으로 변환된다.

```java
@GetMapping("/user")
public User user() {
    return new User("kim", 20);
}
```

응답 메시지 바디는 다음과 같은 형태가 된다.

```json
{
  "name": "kim",
  "age": 20
}
```

반면 문자열을 반환하면 `StringHttpMessageConverter`가 처리한다.

```java
@GetMapping("/hello")
public String hello() {
    return "hello";
}
```

이때는 자바 객체를 JSON으로 변환하는 과정이 아니라, 문자열 자체가 응답 메시지 바디에 작성된다.

따라서 다음과 같이 이해하는 것이 정확하다.

```text
@ResponseBody, @RestController
→ 반환값을 View 이름으로 처리하지 않음
→ 반환값을 HTTP 응답 메시지 바디에 작성함
→ 실제 변환 형식은 HttpMessageConverter가 결정함
```

---

## 3. `ResponseEntity`의 역할

`ResponseEntity`도 메시지 바디에 데이터를 담아 응답한다는 점에서는 `@ResponseBody`와 같다.

다만 `ResponseEntity`는 메시지 바디뿐만 아니라 응답 상태 코드와 응답 헤더까지 직접 설정할 수 있다.

```java
@GetMapping("/user")
public ResponseEntity<User> user() {
    User user = new User("kim", 20);

    return ResponseEntity
            .status(HttpStatus.OK)
            .header("Custom-Header", "custom-value")
            .body(user);
}
```

`ResponseEntity`는 다음 정보를 담을 수 있다.

```text
응답 상태 코드
응답 헤더
응답 메시지 바디
```

위 코드의 `User` 객체도 `ResponseEntity`가 직접 JSON으로 변환하는 것은 아니다.

`ResponseEntity`의 `body`에 담긴 객체를 `HttpMessageConverter`가 JSON 형식으로 변환한다.

```text
ResponseEntity<User>
→ 상태 코드와 헤더 확인
→ body에 들어 있는 User 객체 확인
→ HttpMessageConverter가 JSON 형식으로 변환
→ 응답 메시지 바디에 작성
```

---

## 4. `Accept`와 `Content-Type`의 차이

`Accept`와 `Content-Type`은 둘 다 데이터 형식과 관련된 헤더이지만 의미가 다르다.

### `Accept`

`Accept`는 클라이언트가 서버에 요청을 보낼 때 사용하는 요청 헤더다.

클라이언트가 서버에게 어떤 형식의 응답을 원하는지 알려준다.

```http
GET /users/1 HTTP/1.1
Accept: application/json
```

위 요청은 다음 의미다.

```text
서버야, 응답 메시지 바디를 가능하면 JSON 형식으로 보내줘.
```

### `Content-Type`

`Content-Type`은 현재 전송하는 메시지 바디가 어떤 형식인지 나타낸다.

클라이언트가 요청 메시지 바디를 보낼 때도 사용할 수 있고, 서버가 응답 메시지 바디를 보낼 때도 사용할 수 있다.

요청에서 사용하는 경우:

```http
POST /users HTTP/1.1
Content-Type: application/json

{
  "name": "kim",
  "age": 20
}
```

이 뜻은 다음과 같다.

```text
내가 지금 서버에 보내는 요청 메시지 바디는 JSON 형식이다.
```

응답에서 사용하는 경우:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "name": "kim",
  "age": 20
}
```

이 뜻은 다음과 같다.

```text
서버가 지금 보내는 응답 메시지 바디는 JSON 형식이다.
```

---

## 5. 요청에서는 `Accept`, 응답에서는 `Content-Type`인가

큰 흐름에서는 그렇게 생각할 수 있지만, 요청에서도 `Content-Type`을 사용할 수 있다.

정확히는 다음과 같다.

```text
요청 Accept
→ 클라이언트가 받고 싶은 응답 형식

요청 Content-Type
→ 클라이언트가 서버로 보내는 요청 바디 형식

응답 Content-Type
→ 서버가 클라이언트로 보내는 응답 바디 형식
```

예를 들어 클라이언트가 서버에 JSON 데이터를 보내면서 응답도 JSON으로 받고 싶다면 두 헤더를 모두 사용할 수 있다.

```http
POST /users HTTP/1.1
Content-Type: application/json
Accept: application/json

{
  "name": "kim",
  "age": 20
}
```

각 헤더의 의미는 다음과 같다.

```text
Content-Type: application/json
→ 지금 보내는 요청 메시지 바디가 JSON 형식임

Accept: application/json
→ 서버의 응답 메시지 바디도 JSON 형식으로 받고 싶음
```

---

## 6. GET 요청과 POST 요청에서 자주 사용하는 헤더

GET 요청은 일반적으로 요청 메시지 바디가 없다.

따라서 GET 요청에서는 요청 바디의 형식을 나타내는 `Content-Type`이 크게 필요하지 않다.

```http
GET /users/1 HTTP/1.1
Accept: application/json
```

반면 POST, PUT, PATCH 요청은 요청 메시지 바디에 데이터를 담아 보내는 경우가 많다.

따라서 메시지 바디가 어떤 형식인지 알려주는 `Content-Type`이 중요하다.

```http
POST /users HTTP/1.1
Content-Type: application/json
Accept: application/json
```

간단하게 정리하면 다음과 같다.

```text
GET
→ 요청 바디가 거의 없음
→ Accept가 주로 의미 있음

POST, PUT, PATCH
→ 요청 바디가 있음
→ Content-Type이 중요함
→ 응답 형식을 지정하고 싶으면 Accept도 함께 사용함
```

---

## 7. `ResponseEntity`를 반환하면 `Accept`가 바뀌는가

`ResponseEntity`를 반환한다고 해서 요청 헤더의 `Accept`가 `application/json`으로 바뀌는 것은 아니다.

`Accept`는 클라이언트가 서버에 요청할 때 이미 보낸 요청 헤더다.

서버가 요청을 받은 이후에 `ResponseEntity`를 반환한다고 해서 기존 요청 헤더를 변경하는 개념이 아니다.

서버가 응답을 보낼 때 설정하는 것은 응답 헤더다.

```java
@GetMapping("/user")
public ResponseEntity<User> user() {
    User user = new User("kim", 20);

    return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(user);
}
```

여기서 설정하는 것은 다음 응답 헤더다.

```http
Content-Type: application/json
```

따라서 `ResponseEntity`는 `Accept`를 변경하는 객체가 아니라, 응답의 상태 코드와 헤더, 메시지 바디를 구성하는 객체다.

```text
Accept
→ 클라이언트가 요청할 때 지정함

ResponseEntity의 contentType
→ 서버가 응답할 때 Content-Type을 지정함
```

---

## 8. 서버가 JSON을 보낸다는 말의 정확한 의미

서버가 클라이언트에게 JSON을 보낸다고 표현하지만, 자바 객체나 자바스크립트 객체가 네트워크를 그대로 이동하는 것은 아니다.

서버의 자바 객체는 Jackson을 통해 JSON 문법으로 작성된 텍스트 데이터로 직렬화된다.

```java
User user = new User("kim", 20);
```

위 객체는 다음과 같은 JSON 텍스트로 변환된다.

```json
{
  "name": "kim",
  "age": 20
}
```

실제 HTTP 응답은 다음과 같은 형태가 된다.

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"name":"kim","age":20}
```

그리고 네트워크에서는 최종적으로 문자열 자체가 아니라, 해당 문자열이 UTF-8 등의 문자 인코딩에 따라 변환된 바이트 데이터가 전송된다.

```text
자바 객체
→ JSON 형식의 문자열로 직렬화
→ UTF-8 등의 방식으로 바이트 변환
→ HTTP 응답 메시지 바디로 전송
```

따라서 우리가 편하게 말하는 다음 표현은:

```text
서버가 JSON을 보낸다.
```

엄밀히 말하면 다음 의미다.

```text
서버가 객체를 JSON 문법의 텍스트 데이터로 직렬화하고,
이를 바이트 형태로 HTTP 응답 메시지 바디에 담아 보낸다.
```

---

## 9. 전체 흐름

서버에서 `@RestController`, `@ResponseBody`, `ResponseEntity`를 통해 객체를 반환하는 전체 흐름은 다음과 같다.

```text
컨트롤러에서 자바 객체 반환
→ @ResponseBody 또는 @RestController에 의해 응답 바디 처리
→ ResponseEntity라면 상태 코드와 응답 헤더도 함께 처리
→ HttpMessageConverter 선택
→ Jackson이 자바 객체를 JSON 형식의 문자열로 직렬화
→ 문자열을 바이트로 변환
→ HTTP 응답 메시지 바디로 전송
```

---

## 10. `response.getWriter().write()`와 `@ResponseBody`

둘 다 최종적으로 데이터를 HTTP 응답 메시지 바디에 작성한다는 점에서는 같은 원리다.

```text
response.getWriter().write()
→ 개발자가 JSON 변환, 헤더 설정, 응답 바디 작성을 직접 처리

@ResponseBody
→ 스프링의 HttpMessageConverter가 변환과 응답 바디 작성을 자동 처리
```

즉, 목적은 같고 **직접 처리하느냐 스프링이 대신 처리하느냐의 차이**다.

-- 

## 11. 최종 정리

`@ResponseBody`와 `@RestController`는 반환값을 View 이름으로 처리하지 않고 HTTP 응답 메시지 바디에 작성하도록 한다.

객체를 반환하면 `HttpMessageConverter`와 Jackson이 객체를 JSON 형식의 텍스트 데이터로 직렬화한다.

`ResponseEntity`도 메시지 바디의 객체를 JSON으로 변환하는 원리는 같지만, 추가로 응답 상태 코드와 응답 헤더를 직접 설정할 수 있다.

`Accept`는 클라이언트가 어떤 형식의 응답을 원하는지 나타내는 요청 헤더다.

`Content-Type`은 현재 보내는 메시지 바디가 어떤 형식인지 나타내는 헤더다. 요청과 응답 모두에서 사용할 수 있다.

```text
요청 Accept
→ 어떤 응답 형식을 받고 싶은가

요청 Content-Type
→ 지금 서버로 보내는 요청 바디의 형식이 무엇인가

응답 Content-Type
→ 지금 클라이언트로 보내는 응답 바디의 형식이 무엇인가
```

마지막으로 서버가 JSON을 보낸다는 것은 객체 자체를 보내는 것이 아니다.

```text
자바 객체
→ JSON 형식의 문자열
→ 바이트로 전송
```

즉, `@RestController`, `@ResponseBody`, `ResponseEntity`에서 객체를 반환하면 진짜 객체가 네트워크를 타고 이동하는 것이 아니라, 객체를 JSON 문법으로 직렬화한 텍스트 데이터가 바이트 형태로 전송되는 것이다.
