package com.assignment.githubsearcher.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.assignment.githubsearcher.dto.RepositoryListResponse;
import com.assignment.githubsearcher.dto.RepositoryResponse;
import com.assignment.githubsearcher.dto.SearchResponse;
import com.assignment.githubsearcher.exception.GitHubApiException;
import com.assignment.githubsearcher.exception.GlobalExceptionHandler;
import com.assignment.githubsearcher.exception.InvalidSortException;
import com.assignment.githubsearcher.service.GitHubSearchService;
import com.assignment.githubsearcher.service.RepositoryQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GitHubRepositoryControllerTest {

    @Mock
    private GitHubSearchService gitHubSearchService;

    @Mock
    private RepositoryQueryService repositoryQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GitHubRepositoryController(gitHubSearchService, repositoryQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void search_returnsSavedRepositories() throws Exception {
        RepositoryResponse repo = sampleRepo();
        when(gitHubSearchService.searchAndSave(any()))
                .thenReturn(new SearchResponse("Repositories fetched and saved successfully", List.of(repo)));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"spring boot","language":"Java","sort":"stars"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories[0].id").value(123456));
    }

    @Test
    void search_rejectsMissingQuery() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"Java","sort":"stars"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void search_rejectsInvalidSort() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"spring","sort":"watchers"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void search_rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void search_mapsGitHubRateLimit() throws Exception {
        when(gitHubSearchService.searchAndSave(any()))
                .thenThrow(new GitHubApiException("GITHUB_RATE_LIMIT", "rate limited", 403));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"spring boot"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GITHUB_RATE_LIMIT"));
    }

    @Test
    void getRepositories_returnsFilteredResults() throws Exception {
        when(repositoryQueryService.findStored(eq("Java"), eq(100), eq("stars")))
                .thenReturn(new RepositoryListResponse(List.of(sampleRepo())));

        mockMvc.perform(get("/api/github/repositories")
                        .param("language", "Java")
                        .param("minStars", "100")
                        .param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot-example"));
    }

    @Test
    void getRepositories_usesDefaultSort() throws Exception {
        when(repositoryQueryService.findStored(isNull(), isNull(), eq("stars")))
                .thenReturn(new RepositoryListResponse(List.of()));

        mockMvc.perform(get("/api/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories").isArray());
    }

    @Test
    void getRepositories_mapsInvalidSort() throws Exception {
        when(repositoryQueryService.findStored(isNull(), isNull(), eq("watchers")))
                .thenThrow(new InvalidSortException("sort must be one of: stars, forks, updated"));

        mockMvc.perform(get("/api/github/repositories").param("sort", "watchers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private RepositoryResponse sampleRepo() {
        return new RepositoryResponse(
                123456L,
                "spring-boot-example",
                "An example repository for Spring Boot",
                "user123",
                "Java",
                450,
                120,
                Instant.parse("2024-01-01T12:00:00Z")
        );
    }
}
