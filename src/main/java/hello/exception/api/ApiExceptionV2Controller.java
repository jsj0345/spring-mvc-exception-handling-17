package hello.exception.api;

import hello.exception.exception.UserException;
import hello.exception.exhandler.ErrorResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
### `@ResponseStatus`의 `reason` 유무 차이

`@ResponseStatus`는 `reason`을 작성했는지에 따라 처리 방식이 달라짐.

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
```

위와 같이 `reason` 없이 상태 코드만 지정하면 HTTP 상태 코드만 `400`으로 변경함.

즉, `sendError()`를 호출하지 않고 `response.setStatus(400)`과 비슷하게 동작함.

반면 아래와 같이 `reason`을 함께 작성하면 처리 방식이 달라짐.

```java
@ResponseStatus(
    code = HttpStatus.BAD_REQUEST,
    reason = "error.bad"
)
```

`reason` 값이 존재하면 `ResponseStatusExceptionResolver`가 `sendError()`를 호출함.

즉, 아래와 비슷하게 동작한다고 생각하면 됨.

```java
response.sendError(400, "error.bad");
```

따라서 `sendError()`가 호출되면 WAS의 에러 처리 과정으로 이어질 수 있고, `ERROR` 디스패치가 발생하면서 `/error` 등의 에러 처리 경로로 이동할 수 있음.

정리하면 다음과 같음.

* `@ResponseStatus(HttpStatus.BAD_REQUEST)`

  * 상태 코드만 `400`으로 변경
  * `sendError()` 호출 안 함
  * `setStatus()`와 비슷하게 동작

* `@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad")`

  * 상태 코드와 `reason` 지정
  * `sendError()` 호출
  * WAS의 에러 처리 및 `ERROR` 디스패치로 이어질 수 있음

*/

@Slf4j
@RestController
public class ApiExceptionV2Controller {

  @ResponseStatus(HttpStatus.BAD_REQUEST) // 만약 지정을 안해주면, 200 OK가 나옴. ExceptionResolver에서 예외 처리를 했기 때문에 정상적으로 흐름이 이어짐.
  @ExceptionHandler(IllegalArgumentException.class)
  public ErrorResult illegalExHandler(IllegalArgumentException e) { // 자식예외까지 다 잡아줌.
    log.error("[exceptionHandler] ex", e);
    return new ErrorResult("BAD", e.getMessage());
  }

  @ExceptionHandler // 매개변수에 예외를 넣어도 @ExceptionHandler(UserException.class)이랑 똑같음.
  public ResponseEntity<ErrorResult> userExHandler(UserException e) { // 자식예외까지 다 잡아줌.
    log.error("[exceptionHandler] ex", e);
    ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
    return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler
  public ErrorResult exHandler(Exception e) { // 위에서 처리하지 못한 예외를 여기서 다 처리해줌. (실수로 놓친 예외나 공통으로 처리할 것들)
    log.error("[exceptionHandler] ex", e);
    return new ErrorResult("EX", "내부 오류");
  }

  @GetMapping("/api2/members/{id}")
  public MemberDto getMember(@PathVariable("id") String id) {
    if(id.equals("ex")) {
      throw new RuntimeException("잘못된 사용자");
    }

    if(id.equals("bad")) {
      throw new IllegalArgumentException("잘못된 입력값");
    }

    if(id.equals("user-ex")) {
      throw new UserException("사용자 오류");
    }

    return new MemberDto(id, "hello " + id);
    //JSON형식으로 반환하는데 html 에러 페이지가 나오면 문제가 있다.
  }

  @Data
  @AllArgsConstructor
  static class MemberDto {
    private String memberId;
    private String name;
  }


}
