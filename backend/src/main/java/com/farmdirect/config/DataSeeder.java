package com.farmdirect.config;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Seeds required reference data (categories), an admin from env vars, and - only when SEED_SAMPLE_DATA=true - sample farmers/products for development. */
@Component
public class DataSeeder implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
  private final UserRepository users; private final ProductCategoryRepository categories; private final FarmerProfileRepository farmers; private final CustomerProfileRepository customers;
  private final DeliveryProfileRepository deliveries; private final ProductRepository products; private final InventoryRepository inventory; private final PasswordEncoder encoder;
  @Value("${app.admin.email}") String adminEmail; @Value("${app.admin.mobile}") String adminMobile; @Value("${app.admin.password}") String adminPassword;
  @Value("${app.seed-sample-data}") boolean sample;

  public DataSeeder(UserRepository users, ProductCategoryRepository categories, FarmerProfileRepository farmers, CustomerProfileRepository customers, DeliveryProfileRepository deliveries,
                    ProductRepository products, InventoryRepository inventory, PasswordEncoder encoder) {
    this.users = users; this.categories = categories; this.farmers = farmers; this.customers = customers; this.deliveries = deliveries; this.products = products; this.inventory = inventory; this.encoder = encoder;
  }

  @Override @Transactional
  public void run(ApplicationArguments args) {
    for (String n : List.of("Vegetables", "Fruits", "Dairy Products", "Grains and Cereals", "Pulses", "Other Farm Products"))
      if (categories.findByNameIgnoreCase(n).isEmpty()) { ProductCategory c = new ProductCategory(); c.setName(n); categories.save(c); }
    boolean hasEmail = adminEmail != null && !adminEmail.isBlank();
    boolean hasMobile = adminMobile != null && !adminMobile.isBlank();
    boolean hasPassword = adminPassword != null && !adminPassword.isBlank();
    boolean emailExists = hasEmail && users.existsByEmailIgnoreCase(adminEmail);
    boolean mobileExists = hasMobile && users.existsByMobile(adminMobile);
    if (hasPassword && (hasEmail || hasMobile) && !emailExists && !mobileExists) {
      String email = hasEmail ? adminEmail : "admin@farmdirect.local";
      String mobile = hasMobile ? adminMobile : "";
      user("Administrator", email, mobile, adminPassword, Role.ADMIN);
      log.info("Admin account created using {}", hasEmail ? "email " + email : "mobile " + mobile);
    }
    if (sample && !users.existsByEmailIgnoreCase("farmer1@farmdirect.test")) seedSample();
  }

  private User user(String name, String email, String mobile, String pw, Role role) {
    User u = new User(); u.setFullName(name); u.setEmail(email.toLowerCase()); u.setMobile(mobile); u.setPasswordHash(encoder.encode(pw)); u.setRole(role);
    u.setStatus(AccountStatus.ACTIVE); u.setState("Telangana"); u.setDistrict("Warangal"); u.setCity("Warangal"); u.setAddress("Sample address"); u.setPincode("506001");
    return users.save(u);
  }

  /** Development data only. Log in with email or mobile and the sample password. */
  private void seedSample() {
    String pw = "Sample#Passw0rd2026";
    User f1 = user("Ramesh Reddy", "farmer1@farmdirect.test", "9000000001", pw, Role.FARMER), f2 = user("Lakshmi Devi", "farmer2@farmdirect.test", "9000000002", pw, Role.FARMER);
    user("Asha Customer", "customer@farmdirect.test", "9000000003", pw, Role.CUSTOMER); user("Dev Partner", "delivery@farmdirect.test", "9000000004", pw, Role.DELIVERY_PARTNER);
    users.findByEmailIgnoreCase("customer@farmdirect.test").ifPresent(u -> { CustomerProfile c = new CustomerProfile(); c.setUser(u); customers.save(c); });
    users.findByEmailIgnoreCase("delivery@farmdirect.test").ifPresent(u -> { DeliveryProfile d = new DeliveryProfile(); d.setUser(u); d.setVehicleType("Bike"); deliveries.save(d); });
    FarmerProfile a = farm(f1, "Reddy Organic Fields", 17.9689, 79.5941), b = farm(f2, "Devi Dairy and Millets", 17.9784, 79.6005);
    item(a, "Tomatoes", "Vegetables", "32", Unit.KILOGRAM, "120"); item(a, "Green Chillies", "Vegetables", "60", Unit.KILOGRAM, "40"); item(a, "Mangoes (Banganapalli)", "Fruits", "90", Unit.KILOGRAM, "80");
    item(b, "Cow Milk", "Dairy Products", "58", Unit.LITRE, "50"); item(b, "Ragi", "Grains and Cereals", "75", Unit.KILOGRAM, "200"); item(b, "Toor Dal", "Pulses", "130", Unit.KILOGRAM, "100");
    log.info("Sample development data created (password for all sample users: {})", pw);
  }
  private FarmerProfile farm(User u, String name, double lat, double lng) {
    FarmerProfile f = new FarmerProfile(); f.setUser(u); f.setFarmName(name); f.setFarmAddress("Warangal Rural"); f.setFarmLocation("Warangal, Telangana"); f.setFarmingCategory("Mixed farming"); f.setLatitude(lat); f.setLongitude(lng);
    f.setVerificationStatus(VerificationStatus.APPROVED); return farmers.save(f);
  }
  private void item(FarmerProfile f, String name, String cat, String price, Unit unit, String stock) {
    Product p = new Product(); p.setFarmer(f); p.setCategory(categories.findByNameIgnoreCase(cat).orElseThrow()); p.setName(name); p.setPrice(new BigDecimal(price)); p.setUnit(unit);
    p.setDescription("Grown and harvested on our farm."); p.setHarvestDate(LocalDate.now().minusDays(1)); p = products.save(p);
    Inventory i = new Inventory(); i.setProduct(p); i.setAvailable(new BigDecimal(stock)); inventory.save(i);
  }
}
