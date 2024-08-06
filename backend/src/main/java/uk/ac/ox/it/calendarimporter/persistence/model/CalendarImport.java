package uk.ac.ox.it.calendarimporter.persistence.model;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import uk.ac.ox.it.calendarimporter.Views;
import uk.ac.ox.it.calendarimporter.controller.ImportType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.Instant;

/**
 * This holds details of an import that has been made. From here you can find the jobs related to
 * the initial import and possibly to the removal job.
 */
@Entity
@Data
public class CalendarImport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonView(Views.Public.class)
    private long id;

    /**
     * The context in which the import is to be done. Typically this is the course although in the
     * future we may wish to support importing into a user's calendar. Example: course_123.
     */
    private String context;

    /**
     * The user who created this import.
     */
    @JsonView(Views.Public.class)
    @ManyToOne
    private User user;
    /**
     * When this was created.
     */
    @JsonView(Views.Public.class)
    private Instant created;
    /**
     * The url of the import.
     */
    private String url;

    /**
     * The name of the course/section into which this import was done.
     */
    @JsonView(Views.Public.class)
    private String destinationName;
    /**
     * The ID the of the course/section into which this import was done.
     */
    @JsonView(Views.Public.class)
    private String destinationId;

    /**
     * The filename of the file uploaded
     */
    @JsonView(Views.Public.class)
    private String filename;

    /**
     * The type of importer used.
     */
    @JsonView(Views.Public.class)
    private ImportType type;

    /**
     * The job used to load the data.
     */
    @JsonView(Views.Public.class)
    @OneToOne()
    private JobProgress load;

    /**
     * The job used to remove the data.
     */
    @JsonView(Views.Public.class)
    @OneToOne()
    private JobProgress delete;
}
