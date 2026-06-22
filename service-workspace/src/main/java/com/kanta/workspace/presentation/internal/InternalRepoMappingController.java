package com.kanta.workspace.presentation.internal;

import com.kanta.workspace.application.workspace.WorkspaceService;
import com.kanta.workspace.common.ApiResponse;
import com.kanta.workspace.presentation.workspace.RepoBoardMappingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/repo-mappings")
public class InternalRepoMappingController {
    private final WorkspaceService workspaceService;

    public InternalRepoMappingController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/lookup")
    public ApiResponse<RepoBoardMappingResponse> lookup(@RequestParam String githubRepo) {
        return ApiResponse.ok(workspaceService.findRepoBoardMapping(githubRepo));
    }
}
