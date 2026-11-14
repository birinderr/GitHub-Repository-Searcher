package com.assignment.githubsearcher.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubApiResponse(
        List<GitHubRepoItem> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRepoItem(
            Long id,
            String name,
            String description,
            GitHubOwner owner,
            String language,
            @JsonProperty("stargazers_count") Integer stargazersCount,
            @JsonProperty("forks_count") Integer forksCount,
            @JsonProperty("updated_at") Instant updatedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubOwner(
            String login
    ) {
    }
}
