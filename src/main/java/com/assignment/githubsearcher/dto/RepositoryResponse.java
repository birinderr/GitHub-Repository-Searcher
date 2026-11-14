package com.assignment.githubsearcher.dto;

import com.assignment.githubsearcher.entity.GitHubRepository;
import java.time.Instant;

/**
 * What we send back in the API JSON.
 * We don't return the entity directly — keeps the DB model separate from the API contract.
 */
public record RepositoryResponse(
        Long id,
        String name,
        String description,
        String owner,
        String language,
        Integer stars,
        Integer forks,
        Instant lastUpdated
) {

    public static RepositoryResponse fromEntity(GitHubRepository entity) {
        return new RepositoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerName(),
                entity.getLanguage(),
                entity.getStars(),
                entity.getForks(),
                entity.getLastUpdated()
        );
    }
}
