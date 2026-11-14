package com.assignment.githubsearcher.service;

import com.assignment.githubsearcher.exception.InvalidSortException;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

final class RepositorySortSupport {

    private static final Set<String> ALLOWED = Set.of("stars", "forks", "updated");

    private RepositorySortSupport() {
    }

    static Sort resolve(String sort) {
        String normalized = StringUtils.hasText(sort) ? sort.trim().toLowerCase(Locale.ROOT) : "stars";
        if (!ALLOWED.contains(normalized)) {
            throw new InvalidSortException("sort must be one of: stars, forks, updated");
        }

        return switch (normalized) {
            case "forks" -> Sort.by(Sort.Direction.DESC, "forks");
            case "updated" -> Sort.by(Sort.Direction.DESC, "lastUpdated");
            default -> Sort.by(Sort.Direction.DESC, "stars");
        };
    }
}
