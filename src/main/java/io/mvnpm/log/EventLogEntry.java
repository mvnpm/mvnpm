package io.mvnpm.log;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;

import io.mvnpm.mavencentral.sync.Stage;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * Log Entry
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Entity
public class EventLogEntry extends PanacheEntity {
    public String groupId;
    public String artifactId;
    public String version;
    public LocalDateTime time;
    public Stage stage;
    public String message;
    public String color;

}
