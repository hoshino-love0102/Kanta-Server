package com.kanta.github.presentation.commitlink;

import java.util.UUID;

public record RejectCommitLinkResponse(UUID id, String matchStatus, UUID newCardId) {
}
