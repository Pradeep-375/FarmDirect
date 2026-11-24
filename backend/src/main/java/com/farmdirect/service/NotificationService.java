package com.farmdirect.service;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Map;

@Service
public class NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  private final NotificationRepository repo; private final JavaMailSender mail; private final SmsGateway sms; private final SimpMessagingTemplate ws;
  private final Dispatcher dispatcher;
  @Value("${app.mail-from}") String from;

  public NotificationService(NotificationRepository repo, JavaMailSender mail, SmsGateway sms, SimpMessagingTemplate ws, Dispatcher dispatcher) {
    this.repo = repo; this.mail = mail; this.sms = sms; this.ws = ws; this.dispatcher = dispatcher;
  }

  /** In-app notification (always) plus email and optional SMS for verified contacts. */
  public void notify(User u, String title, String message, String type, boolean alsoSms) {
    Notification n = new Notification();
    n.setUser(u); n.setTitle(title); n.setMessage(message); n.setType(type);
    Notification saved = repo.save(n);
    afterCommit(() -> {
      ws.convertAndSendToUser(String.valueOf(u.getId()), "/queue/notifications",
        Map.of("id", saved.getId(), "title", title, "message", message, "type", type));
      dispatcher.email(from, u.getEmail(), "FarmDirect: " + title, message);
      if (alsoSms) dispatcher.sms(u.getMobile(), "FarmDirect: " + message);
    });
  }

  private void afterCommit(Runnable r) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { r.run(); } });
    } else r.run();
  }

  @Service
  public static class Dispatcher {
    private final JavaMailSender mail; private final SmsGateway sms;
    public Dispatcher(JavaMailSender mail, SmsGateway sms) { this.mail = mail; this.sms = sms; }
    @Async public void email(String from, String to, String subject, String body) {
      try { SimpleMailMessage m = new SimpleMailMessage(); m.setFrom(from); m.setTo(to); m.setSubject(subject); m.setText(body); mail.send(m); }
      catch (Exception e) { log.warn("Email notification failed: {}", e.getClass().getSimpleName()); }
    }
    @Async public void sms(String mobile, String text) {
      try { sms.sendText(mobile, text); } catch (Exception e) { log.warn("SMS notification failed: {}", e.getMessage()); }
    }
  }
}
