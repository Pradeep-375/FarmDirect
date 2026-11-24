package com.farmdirect.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String h = req.getHeader("Authorization");
    if (h != null && h.startsWith("Bearer ")) {
      AuthUser u = jwt.parse(h.substring(7));
      if (u != null) {
        var auth = new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(req, res);
  }
}
