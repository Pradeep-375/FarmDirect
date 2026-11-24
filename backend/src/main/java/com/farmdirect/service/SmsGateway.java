package com.farmdirect.service;

import com.farmdirect.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

/** Real SMS delivery through MSG91 (default) or Twilio. Fails loudly when not configured - never simulates delivery. */
@Service
public class SmsGateway {
  private final RestClient http = RestClient.create();
  @Value("${app.sms.provider}") String provider;
  @Value("${app.sms.msg91-auth-key}") String msg91Key;
  @Value("${app.sms.msg91-notify-template-id}") String msg91Notify;
  @Value("${app.sms.twilio-account-sid}") String twSid;
  @Value("${app.sms.twilio-auth-token}") String twToken;
  @Value("${app.sms.twilio-from}") String twFrom;

  public void sendText(String mobile10, String text) {
    if ("twilio".equalsIgnoreCase(provider)) twilio(mobile10, text);
    else msg91(mobile10, msg91Notify, Map.of("mobiles", "91" + mobile10, "message", text));
  }

  private void msg91(String mobile, String template, Map<String, String> recipient) {
    if (blank(msg91Key) || blank(template)) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS service is not configured");
    try {
      http.post().uri("https://control.msg91.com/api/v5/flow").header("authkey", msg91Key).contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("template_id", template, "short_url", "0", "recipients", List.of(recipient))).retrieve().toBodilessEntity();
    } catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not send SMS"); }
  }

  private void twilio(String mobile, String body) {
    if (blank(twSid) || blank(twToken) || blank(twFrom)) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS service is not configured");
    var form = new LinkedMultiValueMap<String, String>();
    form.add("To", "+91" + mobile); form.add("From", twFrom); form.add("Body", body);
    try {
      http.post().uri("https://api.twilio.com/2010-04-01/Accounts/" + twSid + "/Messages.json").headers(h -> h.setBasicAuth(twSid, twToken))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
    } catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not send SMS"); }
  }
  private static boolean blank(String s) { return s == null || s.isBlank(); }
}
