package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
    UUID memberId,
    String userId,
    String displayName,
    String role,
    String githubUsername,
    Instant joinedAt
) {
    public static MemberResponse from(WorkspaceMember member) {
        return new MemberResponse(
            member.getId(),
            member.getUserId(),
            member.getDisplayName(),
            member.getRole().name(),
            member.getGithubUsername(),
            member.getJoinedAt()
        );
    }
}
