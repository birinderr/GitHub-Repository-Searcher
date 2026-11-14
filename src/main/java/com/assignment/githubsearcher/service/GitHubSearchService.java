package com.assignment.githubsearcher.service;

import com.assignment.githubsearcher.client.GitHubApiClient;
import com.assignment.githubsearcher.client.GitHubSearchResult;
import com.assignment.githubsearcher.dto.RepositoryResponse;
import com.assignment.githubsearcher.dto.SearchRequest;
import com.assignment.githubsearcher.dto.SearchResponse;
import com.assignment.githubsearcher.entity.GitHubRepository;
import com.assignment.githubsearcher.repository.GitHubRepositoryJpaRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubSearchService {

    private static final Logger log = LoggerFactory.getLogger(GitHubSearchService.class);

    private final GitHubApiClient gitHubApiClient;
    private final GitHubRepositoryJpaRepository repository;

    public GitHubSearchService(
            GitHubApiClient gitHubApiClient,
            GitHubRepositoryJpaRepository repository) {
        this.gitHubApiClient = gitHubApiClient;
        this.repository = repository;
    }

    @Transactional
    public SearchResponse searchAndSave(SearchRequest request) {
        log.info(
                "Search requested: query='{}', language='{}', sort='{}'",
                request.query(),
                request.language(),
                request.sort()
        );

        List<GitHubSearchResult> results = gitHubApiClient.searchRepositories(
                request.query(),
                request.language(),
                request.sort()
        );

        List<RepositoryResponse> saved = results.stream()
                .map(this::upsert)
                .map(RepositoryResponse::fromEntity)
                .toList();

        if (saved.isEmpty()) {
            log.info("No repositories found for query='{}'", request.query());
            return new SearchResponse("No repositories found for the given search criteria", saved);
        }

        log.info("Saved {} repositories for query='{}'", saved.size(), request.query());
        return new SearchResponse("Repositories fetched and saved successfully", saved);
    }

    /**
     * Same GitHub id → save() updates the row instead of inserting a duplicate.
     */
    private GitHubRepository upsert(GitHubSearchResult result) {
        Instant lastUpdated = result.lastUpdated() != null ? result.lastUpdated() : Instant.now();

        GitHubRepository entity = new GitHubRepository(
                result.id(),
                result.name(),
                truncateDescription(result.description()),
                result.ownerLogin(),
                result.language(),
                result.stars(),
                result.forks(),
                lastUpdated
        );

        return repository.save(entity);
    }

    /**
     * GitHub documents a 350-char limit, but search API can still return longer legacy text.
     */
    private String truncateDescription(String description) {
        if (description == null || description.length() <= GitHubRepository.DESCRIPTION_MAX_LENGTH) {
            return description;
        }
        log.debug(
                "Truncating description from {} to {} characters",
                description.length(),
                GitHubRepository.DESCRIPTION_MAX_LENGTH
        );
        return description.substring(0, GitHubRepository.DESCRIPTION_MAX_LENGTH);
    }
}
