package com.assignment.githubsearcher.client;

import java.time.Instant;

/**
 * Clean internal model after parsing GitHub's response.
 * Next step will turn these into entities and save them.
 */
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
