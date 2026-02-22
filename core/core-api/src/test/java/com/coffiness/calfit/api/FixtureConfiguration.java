package com.coffiness.calfit.api;

import com.coffiness.calfit.api.fixture.UserFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@TestConfiguration
public class FixtureConfiguration {

  @Bean
  @Scope("prototype")
  UserFixture userFixture(Environment environment, ObjectMapper objectMapper) {
    return UserFixture.create(environment, objectMapper);
  }
}
