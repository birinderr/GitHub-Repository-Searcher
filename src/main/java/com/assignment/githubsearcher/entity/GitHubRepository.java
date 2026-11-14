package com.assignment.githubsearcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Maps one GitHub repo to one row in PostgreSQL.
 * The GitHub repository id is our primary key so re-saving the same repo updates it (upsert later).
 */
@Entity
@Table(name = "github_repositories")
public class GitHubRepository {

    /** GitHub's documented repository description max length. */
    public static final int DESCRIPTION_MAX_LENGTH = 350;

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    private String language;

    @Column(nullable = false)
    private Integer stars;

    @Column(nullable = false)
    private Integer forks;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected GitHubRepository() {
        // Required by JPA
    }

    public GitHubRepository(
            Long id,
            String name,
            String description,
            String ownerName,
            String language,
            Integer stars,
            Integer forks,
            Instant lastUpdated) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerName = ownerName;
        this.language = language;
        this.stars = stars;
        this.forks = forks;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getStars() {
        return stars;
    }

    public Integer getForks() {
        return forks;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
