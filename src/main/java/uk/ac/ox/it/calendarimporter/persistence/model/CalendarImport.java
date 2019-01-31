package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Data;
import uk.ac.ox.it.calendarimporter.controller.ImportType;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.time.Instant;

/**
 * This holds details of an import that has been made.
 */
@Entity
@Data
public class CalendarImport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String context;

    /**
     * The user who created this import.
     */
    @ManyToOne
    private User user;
    /**
     * When this was created.
     */
    private Instant created;
    /**
     * The URL of the import.
     */
    private String url;

    private String filename;
    /**
     * The type of importer used.
     */
    private ImportType type;

    /**
     * The job used to load the data.
     */
    @OneToOne
    private JobProgress load;

    /**
     * The job used to remove the data.
     */
    @OneToOne
    private JobProgress delete;

}
