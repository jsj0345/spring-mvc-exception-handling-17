package hello.exception.api;

import hello.exception.exception.UserException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ApiExceptionV3Controller {

  @GetMapping("/api3/members/{id}")
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
