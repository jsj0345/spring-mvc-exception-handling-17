package hello.exception.resolver;

import hello.exception.exception.UserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UserHandlerExceptionResolver implements HandlerExceptionResolver {

  /*
   * MVC 1편에서 사용했던 ObjectMapper를 사용한다.
   *
   * 복습
   *
   * 클라이언트가 서버로 요청을 보낼 때 요청 메시지 바디에 JSON 데이터가 들어 있으면,
   * JSON 데이터를 자바 객체로 변환하는 과정이 필요하다.
   *
   * 직접 변환한다면 ObjectMapper의 readValue() 등을 사용할 수 있다.
   *
   * 하지만 스프링 MVC에서 @RequestBody를 사용하면,
   * 스프링이 HttpMessageConverter를 통해 요청 메시지 바디를 읽고
   * 내부적으로 ObjectMapper를 사용하여 JSON을 자바 객체로 변환한다.
   *
   * 즉, @RequestBody를 사용한다고 ObjectMapper 변환 과정이 없어지는 것은 아니며,
   * 개발자가 ObjectMapper를 직접 호출하는 코드를 생략할 수 있는 것이다.
   *
   * 반대로 @ResponseBody, @RestController, ResponseEntity를 통해
   * 자바 객체를 응답 메시지 바디에 반환할 때도
   * HttpMessageConverter가 내부적으로 ObjectMapper를 사용하여
   * 자바 객체를 JSON 형식으로 변환한다.
   *
   * 요청: JSON → 자바 객체 (역직렬화)
   * 응답: 자바 객체 → JSON (직렬화)
   */

  private final ObjectMapper objectMapper = new ObjectMapper();

  // 아래 메서드 내용 이해가 좀 안되면 spring-mvc-response-body-json.md 참고
  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {

      if (ex instanceof UserException) {
        log.info("UserException resolver to 400");
        String acceptHeader = request.getHeader("accept");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        if ("application/json".equals(acceptHeader)) {
          Map<String, Object> errorResult = new HashMap<>();
          errorResult.put("ex", ex.getClass());
          errorResult.put("message", ex.getMessage());

          String result = objectMapper.writeValueAsString(errorResult); // JSON 문자열로 변환.

          /*
           * 원래 @RestController, @ResponseBody, ResponseEntity를 사용할 때는
           * 스프링의 HttpMessageConverter가 자바 객체를 JSON으로 변환해서
           * 응답 메시지 바디에 작성해준다.
           *
           * 이때 내부적으로 ObjectMapper가 사용되므로,
           * 개발자가 직접 JSON 문자열로 변환할 필요가 없다.
           *
           * 하지만 여기서는 HttpServletResponse를 이용해
           * 응답 메시지 바디에 직접 데이터를 작성하고 있다.
           *
           * response.getWriter().write()에는 자바 객체를 바로 넣을 수 없으므로,
           * ObjectMapper를 사용해 errorResult 객체를 JSON 문자열로 변환한 뒤
           * 응답 메시지 바디에 직접 작성해야 한다.
           */

          response.setContentType("application/json");
          response.setCharacterEncoding("utf-8");
          response.getWriter().write(result);

          return new ModelAndView();
        } else {
          // TEXT/HTML
          return new ModelAndView("error/500");
        }
      }

    } catch (IOException e) {
        log.error("resolver ex", e);
    }

    return null;
  }
}
