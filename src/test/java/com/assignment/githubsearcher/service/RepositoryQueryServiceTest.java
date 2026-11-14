package com.assignment.githubsearcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.assignment.githubsearcher.dto.RepositoryListResponse;
import com.assignment.githubsearcher.entity.GitHubRepository;
import com.assignment.githubsearcher.exception.InvalidSortException;
import com.assignment.githubsearcher.repository.GitHubRepositoryJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class RepositoryQueryServiceTest {

    @Mock
    private GitHubRepositoryJpaRepository repository;

    @InjectMocks
    private RepositoryQueryService repositoryQueryService;

    @Test
    void findStored_appliesLanguageAndMinStarsWithStarsSort() {
        GitHubRepository entity = sampleEntity();
        when(repository.findByFilters(eq("java"), eq(100), eq(Sort.by(Sort.Direction.DESC, "stars"))))
                .thenReturn(List.of(entity));

        RepositoryListResponse response = repositoryQueryService.findStored("Java", 100, "stars");

        assertThat(response.repositories()).hasSize(1);
        assertThat(response.repositories().get(0).name()).isEqualTo("demo");
    }

    @Test
    void findStored_sortsByForksAndUpdated() {
        when(repository.findByFilters(isNull(), isNull(), eq(Sort.by(Sort.Direction.DESC, "forks"))))
                .thenReturn(List.of());
        when(repository.findByFilters(isNull(), isNull(), eq(Sort.by(Sort.Direction.DESC, "lastUpdated"))))
                .thenReturn(List.of());

        assertThat(repositoryQueryService.findStored(null, null, "forks").repositories()).isEmpty();
        assertThat(repositoryQueryService.findStored(null, null, "updated").repositories()).isEmpty();
    }

    @Test
    void findStored_defaultsSortWhenBlank() {
        when(repository.findByFilters(isNull(), isNull(), eq(Sort.by(Sort.Direction.DESC, "stars"))))
                .thenReturn(List.of());

        assertThat(repositoryQueryService.findStored("  ", null, "  ").repositories()).isEmpty();
    }

    @Test
    void findStored_rejectsInvalidSort() {
        assertThatThrownBy(() -> repositoryQueryService.findStored(null, null, "watchers"))
                .isInstanceOf(InvalidSortException.class);
    }

    @Test
    void findStored_rejectsNegativeMinStars() {
        assertThatThrownBy(() -> repositoryQueryService.findStored(null, -1, "stars"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private GitHubRepository sampleEntity() {
        return new GitHubRepository(
                1L, "demo", "desc", "owner", "Java", 200, 10,
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }
}
