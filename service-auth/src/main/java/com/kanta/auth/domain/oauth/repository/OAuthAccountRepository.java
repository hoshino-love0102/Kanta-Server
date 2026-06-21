package com.kanta.auth.domain.oauth.repository;

import com.kanta.auth.domain.oauth.entity.OAuthAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuthAccount> findByUserId(UUID userId);
}
