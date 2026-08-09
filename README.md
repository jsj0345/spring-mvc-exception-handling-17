# Spring MVC Exception 17

Spring MVC에서 발생하는 예외가 서블릿과 Spring MVC를 거쳐 처리되는 흐름을 학습하고, 오류 화면과 API 예외 응답을 처리하는 방법을 예제 코드와 문서로 정리한 저장소입니다.

서블릿의 기본 예외 처리부터 Spring Boot가 제공하는 오류 처리 기능, `HandlerExceptionResolver`, `@ExceptionHandler`, `@RestControllerAdvice`를 활용한 API 예외 처리까지 학습했습니다.

## 학습 목적

웹 애플리케이션에서 예외가 발생했을 때 WAS와 Spring MVC 내부에서 어떤 흐름으로 처리되는지 이해하기 위해 정리했습니다.

화면 요청과 API 요청의 오류 처리 차이를 확인하고, 예외에 따라 적절한 HTTP 상태 코드와 오류 응답을 반환하는 방법을 이해하는 데 중점을 두었습니다.

## 학습 내용

- 서블릿 예외 처리와 `sendError()`
- 오류 페이지와 `DispatcherType.ERROR`
- 필터와 인터셉터의 오류 요청 처리
- Spring Boot 기본 오류 처리와 `BasicErrorController`
- API 오류 응답과 `Accept`, `Content-Type`
- `HandlerExceptionResolver`를 활용한 예외 처리
- `@ResponseStatus`와 `ResponseStatusException`
- `DefaultHandlerExceptionResolver`
- `@ExceptionHandler`를 활용한 컨트롤러 예외 처리
- `ResponseEntity`를 활용한 오류 응답
- `@ControllerAdvice`, `@RestControllerAdvice`를 활용한 공통 예외 처리

## 디렉터리 구조

```text
exception
├── src
│   ├── main
│   │   ├── docs
│   │   │   ├── 01-Exception.md
│   │   │   ├── 02-api-exception.md
│   │   │   └── spring-mvc-response-body-json.md
│   │   ├── java
│   │   │   └── hello
│   │   │       └── exception
│   │   │           ├── api
│   │   │           ├── exception
│   │   │           ├── exhandler
│   │   │           ├── filter
│   │   │           ├── interceptor
│   │   │           ├── resolver
│   │   │           └── servlet
│   │   └── resources
│   │       ├── templates
│   │       │   ├── error
│   │       │   └── error-page
│   │       ├── application.properties
│   │       └── messages.properties
│   └── test
├── build.gradle
├── gradlew
├── gradlew.bat
└── settings.gradle
```

## 학습 포인트

- 처리되지 않은 예외와 `sendError()`가 WAS의 오류 처리 과정으로 전달되는 흐름을 확인했습니다.
- `DispatcherType`을 통해 일반 요청과 오류 처리 요청을 구분하고 필터와 인터셉터의 동작 차이를 학습했습니다.
- Spring Boot의 기본 오류 처리와 상태 코드별 오류 페이지 적용 방식을 확인했습니다.
- `HandlerExceptionResolver`를 직접 구현하면서 예외를 HTTP 상태 코드와 응답 데이터로 변환하는 과정을 학습했습니다.
- `@ResponseStatus`, `ResponseStatusException`, `DefaultHandlerExceptionResolver` 등 Spring MVC가 제공하는 기본 예외 처리 기능을 학습했습니다.
- `@ExceptionHandler`와 `@RestControllerAdvice`를 활용해 API 예외 처리 로직을 컨트롤러와 분리하고 공통화하는 방법을 익혔습니다.
- `HttpMessageConverter`와 Jackson을 통해 오류 객체가 JSON 응답으로 변환되는 흐름을 함께 복습했습니다.

## 실행 환경

- Java 17
- Spring Boot 4.1.0
- Spring MVC
- Thymeleaf
- Gradle
- Lombok
- Slf4j
- JUnit 5
- IntelliJ IDEA

## 참고

- 코드 출처 : 스프링 MVC 2편 - 백엔드 웹 개발 활용 기술