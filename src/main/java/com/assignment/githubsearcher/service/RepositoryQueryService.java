package com.assignment.githubsearcher.service;

import com.assignment.githubsearcher.dto.RepositoryListResponse;
import com.assignment.githubsearcher.dto.RepositoryResponse;
import com.assignment.githubsearcher.repository.GitHubRepositoryJpaRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RepositoryQueryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryQueryService.class);

    private final GitHubRepositoryJpaRepository repository;

    public RepositoryQueryService(GitHubRepositoryJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RepositoryListResponse findStored(String language, Integer minStars, String sort) {
        if (minStars != null && minStars < 0) {
            throw new IllegalArgumentException("minStars must be greater than or equal to 0");
        }

        Sort resolvedSort = RepositorySortSupport.resolve(sort);
        String languageFilter = StringUtils.hasText(language)
                ? language.trim().toLowerCase(Locale.ROOT)
                : null;

        List<RepositoryResponse> repositories = repository
                .findByFilters(languageFilter, minStars, resolvedSort)
                .stream()
                .map(RepositoryResponse::fromEntity)
                .toList();

        log.info(
                "Retrieved {} stored repositories (language={}, minStars={}, sort={})",
                repositories.size(),
                languageFilter,
                minStars,
                sort
        );

        return new RepositoryListResponse(repositories);
    }
}
