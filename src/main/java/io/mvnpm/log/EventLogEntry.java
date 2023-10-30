package io.mvnpm.log;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import io.mvnpm.mavencentral.sync.Stage;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * Log Entry
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "EventLogEntry.getByGav", query = "from EventLogEntry where groupId = ?1 and artifactId = ?2 and version = ?3 order by time")
})
public class EventLogEntry extends PanacheEntity {
    public String groupId;
    public String artifactId;
    public String version;
    public LocalDateTime time;
    public Stage stage;
    @Column(columnDefinition = "text", length = 10485760)
    public String message;
    public String color;

    public static List<EventLogEntry> findByGav(String groupId, String artifactId, String version) {
        return find("#EventLogEntry.getByGav", groupId, artifactId, version).list();
    }
}
