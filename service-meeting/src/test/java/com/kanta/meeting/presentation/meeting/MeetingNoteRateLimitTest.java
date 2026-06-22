package com.kanta.meeting.presentation.meeting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kanta.meeting.application.meeting.MeetingNoteService;
import com.kanta.meeting.common.GlobalExceptionHandler;
import com.kanta.meeting.infrastructure.ratelimit.MeetingNoteRateLimitInterceptor;
import com.kanta.meeting.infrastructure.security.Passport;
import com.kanta.meeting.infrastructure.security.PassportHolder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MeetingNoteRateLimitTest {
    private static final int PER_MINUTE_LIMIT = 2;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PassportHolder.set(new Passport("user-1", "user", "MEMBER"));

        var meetingNoteService = mock(MeetingNoteService.class);
        when(meetingNoteService.create(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new CreateMeetingNoteResponse(UUID.randomUUID(), "RECEIVED"));

        var controller = new MeetingNoteController(meetingNoteService);
        var rateLimitInterceptor = new MeetingNoteRateLimitInterceptor(PER_MINUTE_LIMIT, 100);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(rateLimitInterceptor)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() {
        PassportHolder.clear();
    }

    @Test
    void 분당_한도_내의_요청은_정상_처리된다() throws Exception {
        for (int i = 0; i < PER_MINUTE_LIMIT; i++) {
            sendCreate().andExpect(status().isAccepted());
        }
    }

    @Test
    void 분당_한도를_초과하면_429와_Retry_After_헤더가_반환된다() throws Exception {
        for (int i = 0; i < PER_MINUTE_LIMIT; i++) {
            sendCreate().andExpect(status().isAccepted());
        }

        sendCreate()
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.status").value(429));
    }

    private ResultActions sendCreate() throws Exception {
        return mockMvc.perform(
            post("/meeting-notes")
                .contentType("application/json")
                .content("{\"boardId\":\"" + UUID.randomUUID() + "\",\"rawText\":\"hello\"}")
        );
    }
}
