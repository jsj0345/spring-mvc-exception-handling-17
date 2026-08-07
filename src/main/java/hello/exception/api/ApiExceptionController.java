package hello.exception.api;

import hello.exception.exception.BadRequestException;
import hello.exception.exception.UserException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

@Slf4j
@RestController
public class ApiExceptionController {

  @GetMapping("/api/members/{id}")
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

  @GetMapping("/api/response-status-ex1")
  public String responseStatusEx1() {
    throw new BadRequestException();
    // ResponseStatusExceptionResolver.java 파일을 보자.
    // applyStatusAndReason 메서드가 있다.
    // response.sendError(statusCode, resolvedReason);
    // return new ModelAndView();
    // 위 코드처럼 상태 코드를 바꾸고 예외처리하고 정상적인 흐름으로 이러감.
    // BadRequestException 클래스를 보면 이해 할 수 있다.
  }

  @GetMapping("/api/response-status-ex2")
  public String responseStatusEx2() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "error.bad", new IllegalArgumentException());
  }

  @GetMapping("/api/default-handler-ex")
  public String defaultException(@RequestParam("data") Integer data) {
    return "ok";
    //DefaultHandlerExceptionResolver 를 봐야함.
    /*
    if (ex instanceof TypeMismatchException theEx) {
        return this.handleTypeMismatch(theEx, request, response, handler);
    }
    이 부분을 보자.
    protected ModelAndView handleTypeMismatch(TypeMismatchException ex, HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
      response.sendError(400);
      return new ModelAndView();
    }
    sendError(400)이여서 500번대가아닌 400번으로 바뀜.
    */
  }

  @Data
  @AllArgsConstructor
  static class MemberDto {
    private String memberId;
    private String name;
  }

}
