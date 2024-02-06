package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * This is an event that has been imported from an external source into Canvas successfully. We are
 * storing details of it so that we can remove all events associated with an import in one go,
 * without having to rely on hidden data in Canvas.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class ImportedEvent {

    @EmbeddedId
    private ImportedEventIdentity identity;
    /**
     * The job which created this event.
     */
    @ManyToOne(optional = false)
    @NotNull
    private CalendarImport calendarImport;
    /**
     * What we consider the status of the event in Canvas.
     */
    private Status status;

    public Integer getId() {
        return identity.getId();
    }

    public enum Status {
        CREATED,
        DELETED,
        MISSING
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ImportedEventIdentity implements Serializable {

        /**
         * The tenant in which the event was created. We need this so that we can remove an event.
         */
        @NotNull
        private Long tenant;

        /**
         * The ID of the Calendar event created in Canvas. This is just unique to a tenant.
         */
        @NotNull
        private Integer id;
    }
}
