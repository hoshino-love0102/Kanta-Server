package com.kanta.workspace.common;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages
) {
}
