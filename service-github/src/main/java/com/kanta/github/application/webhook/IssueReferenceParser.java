package com.kanta.github.application.webhook;

import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class IssueReferenceParser {
    private static final Pattern ISSUE_CODE_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9]{1,9}-\\d+");

    public Optional<String> parse(String branchRef, String commitMessage) {
        var fromBranch = parseFrom(branchRef);
        if (fromBranch.isPresent()) {
            return fromBranch;
        }
        return parseFrom(commitMessage);
    }

    private Optional<String> parseFrom(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        var matcher = ISSUE_CODE_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase());
        }
        return Optional.empty();
    }
}
