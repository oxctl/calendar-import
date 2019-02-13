package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

/**
 * This is an event that has been imported from an external source into Canvas successfully. We are storing details
 * of it so that we can remove all events associated with an import in one go, without having to rely on hidden
 * data in Canvas.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class ImportedEvent {

    public enum Status {CREATED, DELETED, MISSING}

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

}
