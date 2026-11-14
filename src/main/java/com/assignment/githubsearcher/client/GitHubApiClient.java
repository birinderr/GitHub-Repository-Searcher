package com.assignment.githubsearcher.client;

import com.assignment.githubsearcher.exception.GitHubApiException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    private final RestClient restClient;

    public GitHubApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token:}") String token) {
        RestClient.Builder builder = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (StringUtils.hasText(token)) {
            builder = builder.defaultHeader("Authorization", "Bearer " + token);
            log.info("GitHub API client initialized with authentication token");
        } else {
            log.warn("GitHub API client initialized without token — rate limits will be lower");
        }

        this.restClient = builder.build();
    }

    public List<GitHubSearchResult> searchRepositories(String query, String language, String sort) {
        String q = buildSearchQuery(query, language);
        String sortParam = StringUtils.hasText(sort) ? sort.toLowerCase() : "stars";

        log.info("Calling GitHub search API: q='{}', sort='{}'", q, sortParam);

        try {
            GitHubApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/repositories")
                            .queryParam("q", q)
                            .queryParam("sort", sortParam)
                            .queryParam("order", "desc")
                            .queryParam("per_page", 30)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, apiResponse) -> {
                        throw mapStatusError(apiResponse.getStatusCode().value(), apiResponse.getStatusText());
                    })
                    .body(GitHubApiResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                log.info("GitHub search returned no items for q='{}'", q);
                return Collections.emptyList();
            }

            List<GitHubSearchResult> results = response.items().stream()
                    .filter(item -> item != null && item.id() != null)
                    .map(this::toSearchResult)
                    .toList();

            log.info("GitHub search returned {} repositories for q='{}'", results.size(), q);
            return results;
        } catch (GitHubApiException ex) {
            log.warn("GitHub API error [{}]: {}", ex.getCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected failure calling GitHub API for q='{}'", q, ex);
            throw new GitHubApiException(
                    "GITHUB_API_ERROR",
                    "Failed to fetch repositories from GitHub: " + ex.getMessage(),
                    502
            );
        }
    }

    private String buildSearchQuery(String query, String language) {
        StringBuilder builder = new StringBuilder(query.trim());
        if (StringUtils.hasText(language)) {
            builder.append(" language:").append(language.trim());
        }
        return builder.toString();
    }

    private GitHubSearchResult toSearchResult(GitHubApiResponse.GitHubRepoItem item) {
        String owner = Optional.ofNullable(item.owner())
                .map(GitHubApiResponse.GitHubOwner::login)
                .orElse("unknown");

        return new GitHubSearchResult(
                item.id(),
                item.name(),
                item.description(),
                owner,
                item.language(),
                Optional.ofNullable(item.stargazersCount()).orElse(0),
                Optional.ofNullable(item.forksCount()).orElse(0),
                item.updatedAt()
        );
    }

    private GitHubApiException mapStatusError(int status, String body) {
        if (status == 403 || status == 429) {
            return new GitHubApiException(
                    "GITHUB_RATE_LIMIT",
                    "GitHub API rate limit exceeded. Try again later or set GITHUB_TOKEN.",
                    status
            );
        }
        if (status == 422) {
            return new GitHubApiException(
                    "GITHUB_VALIDATION_ERROR",
                    "GitHub rejected the search query.",
                    status
            );
        }
        return new GitHubApiException(
                "GITHUB_API_ERROR",
                "GitHub API request failed with status " + status + ": " + body,
                status >= 500 ? 502 : status
        );
    }
}
