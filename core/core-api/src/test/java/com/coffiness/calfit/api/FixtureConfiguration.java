package com.coffiness.calfit.api;

import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
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

  @Bean
  @Scope("prototype")
  WorkspaceFixture workspaceFixture(Environment environment, ObjectMapper objectMapper) {
    return WorkspaceFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  MemberFixture memberFixture(Environment environment, ObjectMapper objectMapper) {
    return MemberFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  CalendarFixture calendarFixture(Environment environment, ObjectMapper objectMapper) {
    return CalendarFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  MeetingRoomFixture meetingRoomFixture(Environment environment, ObjectMapper objectMapper) {
    return MeetingRoomFixture.create(environment, objectMapper);
  }

  @Bean
  @Scope("prototype")
  AnnouncementBoardFixture announcementBoardFixture(
      Environment environment, ObjectMapper objectMapper) {
    return AnnouncementBoardFixture.create(environment, objectMapper);
  }
}
