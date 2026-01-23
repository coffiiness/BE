package com.team.voteland.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.voteland.api.fixture.UserFixture;
import com.team.voteland.api.fixture.VoteFixture;
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

    @Bean
    @Scope("prototype")
    VoteFixture voteFixture(Environment environment, ObjectMapper objectMapper) {
        return VoteFixture.create(environment, objectMapper);
    }

}
