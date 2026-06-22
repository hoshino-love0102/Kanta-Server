package com.kanta.workspace.presentation.workspace;

import java.util.List;

public record MembersResponse(
    List<MemberResponse> content
) {
}
