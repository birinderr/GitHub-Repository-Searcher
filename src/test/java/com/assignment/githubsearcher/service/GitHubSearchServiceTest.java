package com.assignment.githubsearcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assignment.githubsearcher.client.GitHubApiClient;
import com.assignment.githubsearcher.client.GitHubSearchResult;
import com.assignment.githubsearcher.dto.SearchRequest;
import com.assignment.githubsearcher.dto.SearchResponse;
import com.assignment.githubsearcher.entity.GitHubRepository;
import com.assignment.githubsearcher.repository.GitHubRepositoryJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitHubSearchServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private GitHubRepositoryJpaRepository repository;

    @InjectMocks
    private GitHubSearchService gitHubSearchService;

    @Test
    void searchAndSave_persistsRepositories() {
        GitHubSearchResult result = new GitHubSearchResult(
                123L,
                "spring-boot-example",
                "An example repository",
                "user123",
                "Java",
                450,
                120,
                Instant.parse("2024-01-01T12:00:00Z")
        );

        when(gitHubApiClient.searchRepositories("spring boot", "Java", "stars"))
                .thenReturn(List.of(result));
        when(repository.save(any(GitHubRepository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SearchResponse response = gitHubSearchService.searchAndSave(
                new SearchRequest("spring boot", "Java", "stars")
        );

        assertThat(response.message()).isEqualTo("Repositories fetched and saved successfully");
        assertThat(response.repositories()).hasSize(1);
        assertThat(response.repositories().get(0).owner()).isEqualTo("user123");

        ArgumentCaptor<GitHubRepository> captor = ArgumentCaptor.forClass(GitHubRepository.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(123L);
    }

    @Test
    void searchAndSave_usesNowWhenLastUpdatedMissing() {
        GitHubSearchResult result = new GitHubSearchResult(
                99L, "demo", null, "owner", "Java", 1, 0, null
        );
        when(gitHubApiClient.searchRepositories(eq("demo"), isNull(), isNull()))
                .thenReturn(List.of(result));
        when(repository.save(any(GitHubRepository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SearchResponse response = gitHubSearchService.searchAndSave(new SearchRequest("demo", null, null));

        assertThat(response.repositories()).hasSize(1);
        assertThat(response.repositories().get(0).lastUpdated()).isNotNull();
    }

    @Test
    void searchAndSave_returnsFriendlyMessageWhenEmpty() {
        when(gitHubApiClient.searchRepositories("unknown", null, null)).thenReturn(List.of());

        SearchResponse response = gitHubSearchService.searchAndSave(new SearchRequest("unknown", null, null));

        assertThat(response.message()).isEqualTo("No repositories found for the given search criteria");
        assertThat(response.repositories()).isEmpty();
    }

    @Test
    void searchAndSave_truncatesDescriptionToGitHubLimit() {
        String longDescription = "x".repeat(GitHubRepository.DESCRIPTION_MAX_LENGTH + 50);
        GitHubSearchResult result = new GitHubSearchResult(
                7L, "long-desc", longDescription, "owner", "Java", 1, 0,
                Instant.parse("2024-01-01T00:00:00Z")
        );
        when(gitHubApiClient.searchRepositories("q", null, null)).thenReturn(List.of(result));
        when(repository.save(any(GitHubRepository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SearchResponse response = gitHubSearchService.searchAndSave(new SearchRequest("q", null, null));

        assertThat(response.repositories().get(0).description())
                .hasSize(GitHubRepository.DESCRIPTION_MAX_LENGTH);
    }
}
