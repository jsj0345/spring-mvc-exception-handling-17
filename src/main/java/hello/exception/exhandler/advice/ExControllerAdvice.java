package hello.exception.exhandler.advice;

import hello.exception.exception.UserException;
import hello.exception.exhandler.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "hello.exception.api")
public class ExControllerAdvice {

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


}
