package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @NotNull
    @ManyToOne(optional = false)
    private Tenant tenant;

    @NotNull
    private String context;

    // We need to be able to sort the jobs in the DB.
    @NotNull
    private Instant created;

    @NotNull
    @OneToOne(optional = false)
    private CalendarImport calendarImport ;


}
