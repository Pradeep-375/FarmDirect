package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String h);
  @Modifying @Query("update RefreshToken t set t.revoked=true where t.familyId=:f") void revokeFamily(String f);
  @Modifying @Query("update RefreshToken t set t.revoked=true where t.user.id=:u") void revokeAllForUser(Long u);
}
