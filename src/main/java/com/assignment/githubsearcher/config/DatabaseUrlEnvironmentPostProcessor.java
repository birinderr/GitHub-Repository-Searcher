package com.assignment.githubsearcher.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl.replace("postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                return;
            }

            String username = userInfo.substring(0, userInfo.indexOf(':'));
            String password = userInfo.substring(userInfo.indexOf(':') + 1);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String database = (path != null && path.length() > 1) ? path.substring(1) : path;

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl = jdbcUrl + "?" + uri.getQuery();
            }

            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", jdbcUrl);
            properties.put("spring.datasource.username", username);
            properties.put("spring.datasource.password", password);
            properties.put("DATABASE_USERNAME", username);
            properties.put("DATABASE_PASSWORD", password);

            environment.getPropertySources()
                    .addFirst(new MapPropertySource("renderDatabaseUrl", properties));
        } catch (URISyntaxException ignored) {
        }
    }
}
