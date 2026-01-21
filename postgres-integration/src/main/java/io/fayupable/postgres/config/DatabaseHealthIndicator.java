package io.fayupable.postgres.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component("databaseHealth")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

    private final DataSource dataSource;
    private volatile Boolean lastConnectionState = null;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                if (lastConnectionState == null) {
                    log.info("[Health] Database connection is healthy");
                } else if (!lastConnectionState) {
                    log.info("[Health] Database connection RESTORED");
                }
                lastConnectionState = true;

                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .withDetail("schema", "app_schema")
                        .build();
            }
        } catch (SQLException e) {
            if (lastConnectionState == null || lastConnectionState) {
                log.error("[Health] Database connection failed: {}", e.getMessage());
            }
            lastConnectionState = false;

            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }

        if (lastConnectionState == null || lastConnectionState) {
            log.error("[Health] Database connection validation failed");
        }
        lastConnectionState = false;

        return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", "Connection validation failed")
                .build();
    }
}