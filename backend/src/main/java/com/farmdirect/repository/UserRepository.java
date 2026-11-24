package com.farmdirect.repository;

import com.farmdirect.entity.User;
import com.farmdirect.enums.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmailIgnoreCase(String email);
  Optional<User> findByMobile(String mobile);
  boolean existsByEmailIgnoreCase(String email);
  boolean existsByMobile(String mobile);
  List<User> findByRole(Role role);
  long countByRole(Role role);
}
