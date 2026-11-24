package com.farmdirect.config;

import com.farmdirect.entity.FarmerSubOrder;
import com.farmdirect.enums.Role;
import com.farmdirect.repository.*;
import com.farmdirect.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.web.socket.config.annotation.*;
import java.security.Principal;

/** STOMP over WebSocket. Clients send the JWT in the CONNECT frame's Authorization header. */
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtService jwt; private final FarmerSubOrderRepository subs; private final FarmerProfileRepository farmers;
  @Value("${app.frontend-url}") String frontend;
  public WebSocketConfig(JwtService jwt, FarmerSubOrderRepository subs, FarmerProfileRepository farmers) { this.jwt = jwt; this.subs = subs; this.farmers = farmers; }

  @Override public void configureMessageBroker(MessageBrokerRegistry r) { r.enableSimpleBroker("/topic", "/queue"); r.setUserDestinationPrefix("/user"); r.setApplicationDestinationPrefixes("/app"); }
  @Override public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOrigins(frontend.split(",")); }

  @Override public void configureClientInboundChannel(ChannelRegistration reg) {
    reg.interceptors(new ChannelInterceptor() {
      @Override public Message<?> preSend(Message<?> m, MessageChannel ch) {
        StompHeaderAccessor a = StompHeaderAccessor.wrap(m);
        if (StompCommand.CONNECT.equals(a.getCommand())) {
          String h = a.getFirstNativeHeader("Authorization");
          AuthUser u = (h != null && h.startsWith("Bearer ")) ? jwt.parse(h.substring(7)) : null;
          if (u == null) throw new MessagingException("Unauthorized");
          a.setUser(new java.security.Principal() { public String getName() { return String.valueOf(u.id()); } });
          a.getSessionAttributes().put("role", u.role());
        } else if (StompCommand.SUBSCRIBE.equals(a.getCommand())) {
          Principal p = a.getUser(); String dest = a.getDestination();
          if (p == null || dest == null) throw new MessagingException("Unauthorized");
          if (dest.startsWith("/topic/tracking/")) {
            Long subId = Long.valueOf(dest.substring("/topic/tracking/".length()));
            Long uid = Long.valueOf(p.getName()); Role role = (Role) a.getSessionAttributes().get("role");
            FarmerSubOrder s = subs.findById(subId).orElseThrow(() -> new MessagingException("Not found"));
            boolean ok = role == Role.ADMIN
              || (role == Role.CUSTOMER && s.getOrder().getCustomer().getId().equals(uid))
              || (role == Role.FARMER && s.getFarmer().getUser().getId().equals(uid));
            if (!ok) throw new MessagingException("Forbidden");
          } else if (!dest.startsWith("/user/")) throw new MessagingException("Forbidden");
        }
        return m;
      }
    });
  }
}
