package com.kanta.github.presentation.commitlink;

import com.kanta.github.application.commitlink.CommitLinkService;
import com.kanta.github.common.ApiResponse;
import com.kanta.github.common.PageResponse;
import com.kanta.github.infrastructure.security.UserAccess;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<PageResponse<PendingCommitLinkResponse>> getPending(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(commitLinkService.getPending(page, size));
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
