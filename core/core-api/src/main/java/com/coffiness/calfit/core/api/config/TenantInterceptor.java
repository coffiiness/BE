package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.storage.db.core.config.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String tenantId = request.getHeader("X-Tenant-ID");

    if (tenantId != null && !tenantId.isBlank()) {
      TenantContext.setTenantId(tenantId);
    } else {
      TenantContext.setTenantId("default");
    }
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    TenantContext.clear();
  }
}
