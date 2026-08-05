package hello.exception;

import hello.exception.filter.LogFilter;
import hello.exception.interceptor.LogInterceptor;
import hello.exception.resolver.MyHandlerExceptionResolver;
import hello.exception.resolver.UserHandlerExceptionResolver;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/*
@Configuration은 내부에 @Component가 포함되어 있으므로 설정 클래스 자체를 스프링 빈으로 등록한다.
또한 클래스 내부의 @Bean 메서드가 반환하는 객체들을 스프링 빈으로 등록할 수 있다.
기본 설정에서는 CGLIB 프록시를 사용하여 @Bean 메서드가 다른 @Bean 메서드를 직접 호출하더라도 새로운 객체를 만들지 않고
스프링 컨테이너에 등록된 빈을 반환하도록 보장한다.

@Controller, @Service, @Repository 안에서도 @Bean을 선언할 수 있다.
다만 설정과 비즈니스 역할이 섞이고, @Configuration의 @Bean 메서드 간 호출 보장과 차이가 있기 때문에
일반적으로는 @Bean을 별도의 @Configuration 클래스에 작성한다.
*/

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // dispatcherType을 세팅 할 수 없다.
    registry.addInterceptor(new LogInterceptor())
        .order(1)
        .addPathPatterns("/**")
        .excludePathPatterns("/css/**", "*.ico", "/error", "/error-page/**");
    // /error-page/**를 추가함으로서 한번 더 거치지 않게 하기 위함. (원래 흐름대로라면 인터셉터도 거쳐야함.)
    // 여기서 필터보다 더 편하다는 것을 알 수 있다.
  }

  @Override
  public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    resolvers.add(new MyHandlerExceptionResolver());
    resolvers.add(new UserHandlerExceptionResolver());
  }

  //@Bean
  public FilterRegistrationBean logFilter() {
    FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new LogFilter());
    filterRegistrationBean.setOrder(1);
    filterRegistrationBean.addUrlPatterns("/*");
    //filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR); // REQUEST, ERROR의 경우에 호출.
    filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST);
    return filterRegistrationBean;
  }



}
