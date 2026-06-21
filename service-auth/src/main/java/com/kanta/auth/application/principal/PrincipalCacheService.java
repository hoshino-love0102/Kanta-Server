package com.kanta.auth.application.principal;

import com.kanta.auth.domain.principal.entity.Principal;
import com.kanta.auth.domain.principal.repository.PrincipalRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrincipalCacheService {
    private final PrincipalRepository principalRepository;

    public PrincipalCacheService(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
    }

    @Transactional
    public Principal upsert(UUID userId, String email, String displayName, String role) {
        var principal = principalRepository.findById(userId)
            .map(existing -> {
                existing.update(email, displayName, role);
                return existing;
            })
            .orElseGet(() -> new Principal(userId, email, displayName, role));

        return principalRepository.save(principal);
    }
}
