package com.bookwise.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is required");
        }

        ParsedDatabaseUrl parsed = ParsedDatabaseUrl.from(databaseUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(parsed.jdbcUrl());
        config.setUsername(parsed.username());
        config.setPassword(parsed.password());
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName("bookwise-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
        private static ParsedDatabaseUrl from(String rawUrl) {
            try {
                URI uri = URI.create(rawUrl);
                if (!Objects.equals("postgresql", uri.getScheme())) {
                    throw new IllegalStateException("DATABASE_URL must use the postgresql scheme");
                }
                String host = requireNonBlank(uri.getHost(), "DATABASE_URL host is missing");
                int port = uri.getPort();
                String path = requireNonBlank(uri.getPath(), "DATABASE_URL database name is missing");
                String database = path.startsWith("/") ? path.substring(1) : path;

                StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
                if (port > 0) {
                    jdbc.append(':').append(port);
                }
                jdbc.append('/').append(database);
                if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                    jdbc.append('?').append(uri.getRawQuery());
                }

                String userInfo = uri.getRawUserInfo();
                if (userInfo == null || userInfo.isBlank() || !userInfo.contains(":")) {
                    throw new IllegalStateException("DATABASE_URL must include username and password");
                }
                String[] parts = userInfo.split(":", 2);
                String username = decode(parts[0]);
                String password = decode(parts[1]);
                return new ParsedDatabaseUrl(jdbc.toString(), username, password);
            } catch (Exception ex) {
                if (ex instanceof IllegalStateException illegalStateException) {
                    throw illegalStateException;
                }
                throw new IllegalStateException("DATABASE_URL is malformed", ex);
            }
        }

        private static String requireNonBlank(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(message);
            }
            return value;
        }

        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }
}
