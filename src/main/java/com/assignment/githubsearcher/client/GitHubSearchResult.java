package com.assignment.githubsearcher.client;

import java.time.Instant;

public record GitHubSearchResult(
        Long id,
        String name,
        String description,
        String ownerLogin,
        String language,
        Integer stars,
        Integer forks,
        Instant lastUpdated
) {
}
