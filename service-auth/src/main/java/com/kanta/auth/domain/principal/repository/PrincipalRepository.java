package com.kanta.auth.domain.principal.repository;

import com.kanta.auth.domain.principal.entity.Principal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrincipalRepository extends JpaRepository<Principal, UUID> {
}
