package com.kanta.github.domain.commitlink.repository;

import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitCardLinkRepository extends JpaRepository<CommitCardLink, UUID> {
    Page<CommitCardLink> findByMatchStatusOrderByCreatedAtAsc(MatchStatus matchStatus, Pageable pageable);
}
