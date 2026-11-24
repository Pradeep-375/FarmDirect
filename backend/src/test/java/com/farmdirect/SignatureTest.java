package com.farmdirect;

import com.farmdirect.service.RazorpayService;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class SignatureTest {
  private String sign(String secret, String data) throws Exception {
    Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256")); return HexFormat.of().formatHex(m.doFinal(data.getBytes()));
  }
  @Test void acceptsValidAndRejectsTampered() throws Exception {
    String sig = sign("s3cret", "order_1|pay_1");
    assertTrue(RazorpayService.hmacMatches("s3cret", "order_1|pay_1", sig));
    assertFalse(RazorpayService.hmacMatches("s3cret", "order_1|pay_2", sig));
    assertFalse(RazorpayService.hmacMatches("other", "order_1|pay_1", sig));
    assertFalse(RazorpayService.hmacMatches("", "order_1|pay_1", sig));
    assertFalse(RazorpayService.hmacMatches("s3cret", "order_1|pay_1", null));
  }
}
