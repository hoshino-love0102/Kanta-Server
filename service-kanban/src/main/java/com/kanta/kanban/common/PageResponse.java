package com.kanta.kanban.common;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages
) {
}
