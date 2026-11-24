package com.farmdirect.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window per-IP limiter for /api/auth. For multi-replica deployments back this with Redis. */
public class RateLimitFilter extends OncePerRequestFilter {
  private record Window(long start, int count) {}
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final int sensitivePerHour, authPerMinute;

  public RateLimitFilter(int sensitivePerHour, int authPerMinute) {
    this.sensitivePerHour = sensitivePerHour; this.authPerMinute = authPerMinute;
  }
  @Override protected boolean shouldNotFilter(HttpServletRequest r) { return !r.getRequestURI().startsWith("/api/auth/"); }

  private boolean allow(String key, long windowSec, int max) {
    long now = Instant.now().getEpochSecond();
    Window w = windows.compute(key, (k, o) -> (o == null || now - o.start >= windowSec) ? new Window(now, 1) : new Window(o.start, o.count + 1));
    if (windows.size() > 50_000) windows.entrySet().removeIf(e -> now - e.getValue().start > 3600);
    return w.count <= max;
  }

  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String ip = req.getRemoteAddr(), path = req.getRequestURI();
    boolean sensitivePath = path.endsWith("/login") || path.endsWith("/register");
    boolean ok = allow("a:" + ip, 60, authPerMinute) && (!sensitivePath || allow("o:" + ip, 3600, sensitivePerHour));
    if (!ok) {
      res.setStatus(429); res.setContentType("application/json");
      res.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
      return;
    }
    chain.doFilter(req, res);
  }
}
