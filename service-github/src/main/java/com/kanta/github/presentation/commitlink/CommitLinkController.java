package com.kanta.github.presentation.commitlink;

import com.kanta.github.application.commitlink.CommitLinkService;
import com.kanta.github.common.ApiResponse;
import com.kanta.github.infrastructure.security.UserAccess;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/github/commit-links")
@UserAccess
public class CommitLinkController {
    private final CommitLinkService commitLinkService;

    public CommitLinkController(CommitLinkService commitLinkService) {
        this.commitLinkService = commitLinkService;
    }

    @GetMapping("/pending")
    public ApiResponse<PendingCommitLinksResponse> getPending() {
        var content = commitLinkService.getPending().stream().map(PendingCommitLinkResponse::from).toList();
        return ApiResponse.ok(new PendingCommitLinksResponse(content));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<ConfirmCommitLinkResponse> confirm(@PathVariable UUID id) {
        var link = commitLinkService.confirm(id);
        return ApiResponse.ok(new ConfirmCommitLinkResponse(link.getId(), link.getMatchStatus().name(), link.getCardId()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<RejectCommitLinkResponse> reject(@PathVariable UUID id) {
        var link = commitLinkService.reject(id);
        return ApiResponse.ok(new RejectCommitLinkResponse(link.getId(), link.getMatchStatus().name(), link.getNewCardId()));
    }
}
