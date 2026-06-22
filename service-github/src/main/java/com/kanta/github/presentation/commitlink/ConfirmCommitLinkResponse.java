package com.kanta.github.presentation.commitlink;

import java.util.UUID;

public record ConfirmCommitLinkResponse(UUID id, String matchStatus, UUID cardId) {
}
