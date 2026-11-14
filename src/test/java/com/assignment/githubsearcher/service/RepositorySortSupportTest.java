package com.assignment.githubsearcher.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class RepositorySortSupportTest {

    @Test
    void resolve_coversAllowedValuesAndPrivateConstructor() throws Exception {
        assertThat(RepositorySortSupport.resolve(null).getOrderFor("stars").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(RepositorySortSupport.resolve("FORKS").iterator().next().getProperty())
                .isEqualTo("forks");
        assertThat(RepositorySortSupport.resolve("updated").iterator().next().getProperty())
                .isEqualTo("lastUpdated");

        Constructor<RepositorySortSupport> constructor = RepositorySortSupport.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
