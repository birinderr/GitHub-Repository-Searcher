package com.assignment.githubsearcher.controller;

import com.assignment.githubsearcher.dto.RepositoryListResponse;
import com.assignment.githubsearcher.dto.SearchRequest;
import com.assignment.githubsearcher.dto.SearchResponse;
import com.assignment.githubsearcher.service.GitHubSearchService;
import com.assignment.githubsearcher.service.RepositoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
@Validated
@Tag(name = "GitHub Repositories", description = "Search GitHub repositories and retrieve stored results")
public class GitHubRepositoryController {

    private final GitHubSearchService gitHubSearchService;
    private final RepositoryQueryService repositoryQueryService;

    public GitHubRepositoryController(
            GitHubSearchService gitHubSearchService,
            RepositoryQueryService repositoryQueryService) {
        this.gitHubSearchService = gitHubSearchService;
        this.repositoryQueryService = repositoryQueryService;
    }

    @PostMapping("/search")
    @Operation(summary = "Search GitHub repositories and persist results")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return gitHubSearchService.searchAndSave(request);
    }

    @GetMapping("/repositories")
    @Operation(summary = "Retrieve stored repositories with optional filters")
    public RepositoryListResponse getRepositories(
            @Parameter(description = "Filter by programming language")
            @RequestParam(required = false) String language,

            @Parameter(description = "Minimum stars count")
            @RequestParam(required = false) @Min(0) Integer minStars,

            @Parameter(description = "Sort by stars, forks, or updated (default: stars)")
            @RequestParam(required = false, defaultValue = "stars") String sort) {
        return repositoryQueryService.findStored(language, minStars, sort);
    }
}
