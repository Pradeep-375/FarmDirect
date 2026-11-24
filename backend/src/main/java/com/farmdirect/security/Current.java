package com.farmdirect.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Current {
  private Current() {}
  public static AuthUser user() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !(a.getPrincipal() instanceof AuthUser u)) throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
    return u;
  }
  public static Long id() { return user().id(); }
}
