package com.kanta.meeting.domain.meeting.repository;

import com.kanta.meeting.domain.meeting.entity.ActionItemCandidate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionItemCandidateRepository extends JpaRepository<ActionItemCandidate, UUID> {
    List<ActionItemCandidate> findByMeetingNoteId(UUID meetingNoteId);

    List<ActionItemCandidate> findByIdInAndMeetingNoteId(List<UUID> ids, UUID meetingNoteId);
}
