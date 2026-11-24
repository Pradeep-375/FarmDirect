package com.farmdirect.config;

import com.farmdirect.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;
import java.util.Arrays;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

  @Bean SecurityFilterChain chain(HttpSecurity http, JwtService jwtService, @Value("${app.rate-limit.sensitive-per-hour-per-ip}") int sensitiveHour, @Value("${app.rate-limit.auth-per-minute-per-ip}") int authMin) throws Exception {
    JwtAuthFilter jwt = new JwtAuthFilter(jwtService); RateLimitFilter rate = new RateLimitFilter(sensitiveHour, authMin);
    http.csrf(c -> c.disable()).cors(c -> {}).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/auth/**", "/api/public/**", "/api/payments/webhook", "/ws/**", "/uploads/**", "/actuator/health", "/health").permitAll()
        .requestMatchers("/api/farmer/**").hasRole("FARMER")
        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/delivery/**").hasRole("DELIVERY_PARTNER")
        .anyRequest().authenticated())
      .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
        res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"status\":401,\"message\":\"Authentication required\"}");
      }))
      .headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(c -> {}))
      .addFilterBefore(rate, UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean CorsConfigurationSource cors(@Value("${app.frontend-url}") String frontend) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.stream(frontend.split(","))
      .map(String::trim)
      .filter(origin -> !origin.isEmpty())
      .toList());
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }
}
