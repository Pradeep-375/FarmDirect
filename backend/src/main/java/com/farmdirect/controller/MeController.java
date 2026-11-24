package com.farmdirect.controller;

import com.farmdirect.entity.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import com.farmdirect.security.Current;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class MeController {
  private final UserRepository users; private final NotificationRepository notifications;
  public MeController(UserRepository users, NotificationRepository notifications) { this.users = users; this.notifications = notifications; }

  public record MeDto(Long id, String fullName, String email, String mobile, String role, String state, String district, String city, String address, String pincode, String preferredLanguage) {}
  public record UpdateMe(@NotBlank String fullName, String state, String district, String city, String address, String pincode, String preferredLanguage) {}
  private MeDto dto(User u) { return new MeDto(u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getRole().name(), u.getState(), u.getDistrict(), u.getCity(), u.getAddress(), u.getPincode(), u.getPreferredLanguage()); }

  @GetMapping("/me") public MeDto me() { return dto(users.findById(Current.id()).orElseThrow()); }
  @PutMapping("/me") @Transactional public MeDto update(@RequestBody @jakarta.validation.Valid UpdateMe r) {
    User u = users.findById(Current.id()).orElseThrow();
    u.setFullName(r.fullName()); u.setState(r.state()); u.setDistrict(r.district()); u.setCity(r.city()); u.setAddress(r.address()); u.setPincode(r.pincode());
    if (r.preferredLanguage() != null) u.setPreferredLanguage(r.preferredLanguage());
    return dto(u);
  }

  public record NotificationDto(Long id, String title, String message, String type, boolean read, java.time.Instant createdAt) {}
  @GetMapping("/notifications") public List<NotificationDto> list() {
    return notifications.findTop50ByUserIdOrderByIdDesc(Current.id()).stream().map(n -> new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt())).toList();
  }
  @PostMapping("/notifications/{id}/read") @Transactional public void read(@PathVariable Long id) {
    Notification n = notifications.findById(id).filter(x -> x.getUser().getId().equals(Current.id())).orElseThrow(() -> ApiException.notFound("Not found")); n.setRead(true);
  }
  @PostMapping("/notifications/read-all") @Transactional public void readAll() { notifications.findTop50ByUserIdOrderByIdDesc(Current.id()).forEach(n -> n.setRead(true)); }
}
