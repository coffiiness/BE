package com.coffiness.calfit.core.api.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

class TenantInterceptorTest {
  private final MemberRepository members = mock(MemberRepository.class);
  private final TenantInterceptor interceptor = new TenantInterceptor(members);

  @AfterEach
  void clear() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void missingHeaderUsesTheSoleMembership() {
    login("MEMBER");
    when(members.findTenantIdByUserId(3L)).thenReturn(Optional.of("A"));
    invoke("GET", "/api/v1/members", null, null);
    assertThat(TenantContext.getTenantId()).isEqualTo("A");
  }

  @Test
  void forgedHeaderAndPathAreRejectedBeforeTenantIsSet() {
    login("MEMBER");
    when(members.findTenantIdByUserId(3L)).thenReturn(Optional.of("A"));
    assertThatThrownBy(() -> invoke("GET", "/api/v1/members", "B", null))
        .isInstanceOf(CoreException.class);
    assertThat(TenantContext.getTenantId()).isNull();
    assertThatThrownBy(() -> invoke("POST", "/api/v1/workspaces/B/invitations", "A", "B"))
        .isInstanceOf(CoreException.class);
  }

  @Test
  void duplicateMembershipIsAnExplicitConflict() {
    login("MEMBER");
    when(members.findTenantIdByUserId(3L)).thenCallRealMethod();
    when(members.findActiveTenantIdsByUserId(3L)).thenReturn(List.of("A", "B"));
    assertThatThrownBy(() -> invoke("GET", "/api/v1/members", null, null))
        .isInstanceOfSatisfying(
            CoreException.class,
            e -> assertThat(e.getCustomCode()).isEqualTo("MULTIPLE_ACTIVE_MEMBERSHIPS"));
  }

  @Test
  void unassignedUsersCanCreateAndAcceptButCannotReadTenantData() {
    login("MEMBER");
    when(members.findTenantIdByUserId(3L)).thenReturn(Optional.empty());
    invoke("POST", "/api/v1/workspaces", "stale", null);
    assertThat(TenantContext.getTenantId()).isEqualTo("default");
    invoke("POST", "/api/v1/invitations/token/accept", null, null);
    invoke("GET", "/api/v1/notifications/stream", "forged", null);
    assertThat(TenantContext.getTenantId()).isEqualTo("default");
    assertThatThrownBy(() -> invoke("GET", "/api/v1/members", null, null))
        .isInstanceOf(CoreException.class);
  }

  @Test
  void adminApplicantPublicAndWebhookFlowsRemainIndependentOfEmployeeMembership() {
    login("ADMIN");
    invoke("GET", "/api/v1/admin/users", null, null);
    login("APPLICANT");
    invoke("POST", "/api/v1/application-files/presign-upload", "A", null);
    login("MEMBER");
    invoke("POST", "/api/v1/users/login", null, null);
    invoke("GET", "/api/v1/application-files/health", null, null);
    invoke("GET", "/api/v1/careers/A", "A", null);
    invoke("POST", "/api/v1/calendars/google/notifications", null, null);
    verifyNoInteractions(members);
    interceptor.afterCompletion(
        new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), null);
    assertThat(TenantContext.getTenantId()).isNull();
  }

  private void login(String role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new SecurityUser(3L, "synthetic@example.invalid", role), "unused", List.of()));
  }

  private void invoke(String method, String path, String header, String target) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    if (header != null) request.addHeader("X-Tenant-ID", header);
    if (target != null)
      request.setAttribute(
          HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("workspaceId", target));
    interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
  }
}
