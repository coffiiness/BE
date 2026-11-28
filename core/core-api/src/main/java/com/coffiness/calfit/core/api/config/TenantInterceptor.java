package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

  private static final String DEFAULT_TENANT = "default";

  private final MemberRepository memberRepository;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    TenantContext.clear();
    String requested = request.getHeader("X-Tenant-ID");
    String path = request.getRequestURI();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof SecurityUser user)
        || "ADMIN".equals(user.role())
        || "APPLICANT".equals(user.role())
        || isPublicFlow(path, request.getMethod())) {
      TenantContext.setTenantId(hasText(requested) ? requested : DEFAULT_TENANT);
      return true;
    }
    String canonical = memberRepository.findTenantIdByUserId(user.userId()).orElse(null);
    if (canonical != null && hasText(requested) && !canonical.equals(requested)) {
      throw new CoreException(
          ErrorType.FORBIDDEN, "TENANT_MISMATCH", "요청한 워크스페이스와 현재 소속이 일치하지 않습니다.");
    }
    boolean bootstrap =
        path.equals("/api/v1/workspaces")
            || path.matches("/api/v1/invitations/[^/]+/accept")
            || path.equals("/api/v1/notifications/stream")
            || path.startsWith("/api/v1/users/");
    if (bootstrap) {
      TenantContext.setTenantId(canonical != null ? canonical : DEFAULT_TENANT);
      return true;
    }
    if (canonical == null) {
      throw new CoreException(
          ErrorType.FORBIDDEN, "ACTIVE_MEMBERSHIP_REQUIRED", "워크스페이스 소속이 필요합니다.");
    }
    Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if (variables instanceof Map<?, ?> values
        && values.get("workspaceId") instanceof String target
        && !canonical.equals(target)) {
      throw new CoreException(
          ErrorType.FORBIDDEN, "TENANT_MISMATCH", "요청한 워크스페이스와 현재 소속이 일치하지 않습니다.");
    }
    TenantContext.setTenantId(canonical);
    return true;
  }

  private boolean isPublicFlow(String path, String method) {
    return path.equals("/api/v1/users/login")
        || path.equals("/api/v1/users/signup")
        || path.equals("/api/v1/users/signup/resend-verification")
        || path.equals("/api/v1/users/signup/verify")
        || path.equals("/api/v1/application-files/health")
        || path.startsWith("/api/v1/careers/")
        || path.equals("/api/v1/careers")
        || path.equals("/api/v1/calendars/google/notifications")
        || ("GET".equals(method) && path.startsWith("/api/v1/invitations/"))
        || path.matches("/api/v1/workspaces/[^/]+/applicants/(signup|login)");
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    TenantContext.clear();
  }
}
