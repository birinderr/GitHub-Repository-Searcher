package com.assignment.githubsearcher.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.assignment.githubsearcher.entity.GitHubRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class GitHubRepositoryJpaRepositoryTest {

    @Autowired
    private GitHubRepositoryJpaRepository repository;

    @Test
    void findByFilters_filtersAndSorts() {
        repository.save(new GitHubRepository(
                1L, "low", "d", "a", "Java", 50, 1, Instant.parse("2024-01-01T00:00:00Z")));
        repository.save(new GitHubRepository(
                2L, "high", "d", "b", "Java", 500, 9, Instant.parse("2024-02-01T00:00:00Z")));
        repository.save(new GitHubRepository(
                3L, "other", "d", "c", "Go", 900, 2, Instant.parse("2024-03-01T00:00:00Z")));

        List<GitHubRepository> results = repository.findByFilters(
                "java", 100, Sort.by(Sort.Direction.DESC, "stars"));

        assertThat(results).extracting(GitHubRepository::getName).containsExactly("high");
    }

    @Test
    void findByFilters_allowsNullFilters() {
        repository.save(new GitHubRepository(
                10L, "any", "d", "a", "Rust", 1, 0, Instant.parse("2024-01-01T00:00:00Z")));

        assertThat(repository.findByFilters(null, null, Sort.by("stars"))).hasSize(1);
    }
}
