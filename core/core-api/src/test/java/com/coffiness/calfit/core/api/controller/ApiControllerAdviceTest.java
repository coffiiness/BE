package com.coffiness.calfit.core.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@SpringJUnitWebConfig(ApiControllerAdviceTest.MvcConfiguration.class)
class ApiControllerAdviceTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mvc;
  private Logger logger;
  private Level previousLevel;
  private ListAppender<ILoggingEvent> events;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    logger = (Logger) LoggerFactory.getLogger(ApiControllerAdvice.class);
    previousLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    events = new ListAppender<>();
    events.start();
    logger.addAppender(events);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(events);
    logger.setLevel(previousLevel);
    events.stop();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/h2-console/", "/actuator/health", "/not-a-real-endpoint"})
  void missingHandlerReturnsExisting404EnvelopeWithoutErrorStack(String path) throws Exception {
    mvc.perform(get(path))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.result").value("ERROR"))
        .andExpect(jsonPath("$.error.code").value("E404"))
        .andExpect(jsonPath("$.error.message").value("Resource not found."))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(NoHandlerFoundException.class));
    assertMissingEndpointLog();
  }

  @Test
  void missingStaticResourceReturns404WithoutErrorStack() throws Exception {
    mvc.perform(get("/assets/not-a-real-file.js"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("E404"))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(NoResourceFoundException.class));
    assertMissingEndpointLog();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/test/server-failure", "/test/illegal-state"})
  void unexpectedServerFailureStillReturns500WithErrorStack(String path) throws Exception {
    mvc.perform(get(path))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code").value("E500"));
    assertThat(events.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.ERROR);
              assertThat(event.getThrowableProxy()).isNotNull();
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"/not-a-real-endpoint", "/assets/not-a-real-file.js"})
  void sseMissingEndpointKeepsEmptyErrorBody(String path) throws Exception {
    mvc.perform(get(path).accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isNotFound())
        .andExpect(content().string(""));
    assertMissingEndpointLog();
  }

  @Test
  void sseServerFailureKeeps500AndEmptyBody() throws Exception {
    mvc.perform(get("/test/server-failure").accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(""));
  }

  private void assertMissingEndpointLog() {
    assertThat(events.list)
        .hasSize(1)
        .allSatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
              assertThat(event.getThrowableProxy()).isNull();
            });
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableWebMvc
  @Import({ApiControllerAdvice.class, FailureController.class})
  static class MvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry
          .addResourceHandler("/assets/**")
          .addResourceLocations("classpath:/missing-test-assets/");
    }
  }

  @RestController
  @TestComponent
  static class FailureController {
    @GetMapping("/test/server-failure")
    String serverFailure() throws Exception {
      throw new Exception("test server failure");
    }

    @GetMapping("/test/illegal-state")
    String illegalState() {
      throw new IllegalStateException("test illegal state");
    }
  }
}
