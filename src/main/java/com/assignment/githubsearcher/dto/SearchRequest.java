package com.assignment.githubsearcher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SearchRequest(
        @NotBlank(message = "query is required")
        String query,

        String language,

        @Pattern(
                regexp = "stars|forks|updated",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "sort must be one of: stars, forks, updated"
        )
        String sort
) {
}
