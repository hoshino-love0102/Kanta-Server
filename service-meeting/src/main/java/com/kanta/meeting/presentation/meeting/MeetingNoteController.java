package com.kanta.meeting.presentation.meeting;

import com.kanta.meeting.application.meeting.MeetingNoteService;
import com.kanta.meeting.common.ApiResponse;
import com.kanta.meeting.infrastructure.security.UserAccess;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@UserAccess
@RestController
@RequestMapping("/meeting-notes")
public class MeetingNoteController {
    private final MeetingNoteService meetingNoteService;

    public MeetingNoteController(MeetingNoteService meetingNoteService) {
        this.meetingNoteService = meetingNoteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateMeetingNoteResponse>> create(
        @Valid @RequestBody CreateMeetingNoteRequest request
    ) {
        return ResponseEntity.status(202).body(ApiResponse.accepted(meetingNoteService.create(request)));
    }

    @GetMapping("/{meetingNoteId}")
    public ApiResponse<MeetingNoteResponse> get(@PathVariable UUID meetingNoteId) {
        return ApiResponse.ok(meetingNoteService.get(meetingNoteId));
    }

    @PostMapping("/{meetingNoteId}/register-cards")
    public ResponseEntity<ApiResponse<RegisterCardsResponse>> registerCards(
        @PathVariable UUID meetingNoteId,
        @Valid @RequestBody RegisterCardsRequest request
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(meetingNoteService.registerCards(meetingNoteId, request)));
    }
}
