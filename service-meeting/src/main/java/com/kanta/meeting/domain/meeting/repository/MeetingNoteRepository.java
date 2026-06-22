package com.kanta.meeting.domain.meeting.repository;

import com.kanta.meeting.domain.meeting.entity.MeetingNote;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingNoteRepository extends JpaRepository<MeetingNote, UUID> {
}
