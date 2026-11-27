package com.coffiness.calfit.api.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.domain.notification.NotificationSseService;
import com.coffiness.calfit.support.security.jwt.JwtTokenProvider;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CalfitApiTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EndpointSecurityRegressionTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenProvider tokens;
  @Autowired private NotificationSseService sse;
  @Autowired private Environment environment;
  @Autowired private DataSource dataSource;
  @Autowired private TestRestTemplate http;

  @Test
  void publicHealthStillReturns200() throws Exception {
    mvc.perform(get("/health")).andExpect(status().isOk());
  }

  @Test
  void protectedRouteWithoutJwtStillReturns401() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  @Test
  void invalidJwtStillReturns401() throws Exception {
    mvc.perform(
            get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.value"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownPrivateRouteRequiresAuthenticationBefore404() throws Exception {
    mvc.perform(get("/api/v1/not-a-real-endpoint")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/not-a-real-endpoint").header(HttpHeaders.AUTHORIZATION, bearer("USER")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("E404"));
  }

  @Test
  void adminRouteStillRejectsRegularUserAndAcceptsAdmin() throws Exception {
    mvc.perform(
            get("/api/v1/admin/dashboard/summary")
                .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    mvc.perform(
            get("/api/v1/admin/dashboard/summary")
                .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("SUCCESS"));
  }

  @Test
  void corsStillAcceptsConfiguredOriginAndRejectsOtherOrigin() throws Exception {
    mvc.perform(
            options("/api/v1/users/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    mvc.perform(
            options("/api/v1/users/me")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void sseConnectionAndHeartbeatStillWork() throws Exception {
    MvcResult stream =
        mvc.perform(
                get("/api/v1/notifications/stream")
                    .header(HttpHeaders.AUTHORIZATION, bearer("USER"))
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andExpect(content().string(containsString("event:connected")))
            .andExpect(header().string("X-Accel-Buffering", "no"))
            .andReturn();
    try {
      sse.sendHeartbeat();
      assertThat(stream.getResponse().getContentAsString()).contains("event:heartbeat");
    } finally {
      stream.getRequest().getAsyncContext().complete();
    }
  }

  @Test
  void unauthenticatedSseStillReturns401() throws Exception {
    mvc.perform(get("/api/v1/notifications/stream").accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void localH2AndConsoleRemainAvailable() throws Exception {
    assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isTrue();
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
    }
    var response = http.getForEntity("/h2-console/", String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).containsIgnoringCase("H2 Console");
  }

  private String bearer(String role) {
    return "Bearer " + tokens.createAccessToken(987654321L, "regression@example.test", role);
  }
}
