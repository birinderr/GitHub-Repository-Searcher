package com.assignment.githubsearcher.repository;

import com.assignment.githubsearcher.entity.GitHubRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GitHubRepositoryJpaRepository extends JpaRepository<GitHubRepository, Long> {

    @Query("""
            SELECT r FROM GitHubRepository r
            WHERE (:language IS NULL OR LOWER(r.language) = :language)
              AND (:minStars IS NULL OR r.stars >= :minStars)
            """)
    List<GitHubRepository> findByFilters(
            @Param("language") String language,
            @Param("minStars") Integer minStars,
            Sort sort);
}
