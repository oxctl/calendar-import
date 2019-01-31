package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * This links an CalendarImport to a context that it's run against.
 */
@Entity
@Data
public class ContextJob {

    @Id
    private long id;

    private String context;

    // We need to be able to sort the jobs in the DB.
    private Instant created;

    @OneToOne
    @NotNull
    private CalendarImport calendarImport ;

    @OneToOne
    private Tenant tenant;

}
