package com.kanta.github.domain.commitlink.repository;

import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitCardLinkRepository extends JpaRepository<CommitCardLink, UUID> {
    List<CommitCardLink> findByMatchStatusOrderByCreatedAtAsc(MatchStatus matchStatus);
}
