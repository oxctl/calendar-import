package uk.ac.ox.it.calendarimporter.persistence.model;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ox.it.calendarimporter.Views;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;

/**
 * Holds the progress of a job that means that we don't have to query quartz to find out what's
 * happened. Job progress has an idea of how "complete" a job is.
 */
@Data
@Entity
@NoArgsConstructor
public class JobProgress {

    /**
     * This is the trigger ID.
     */
    @JsonView(Views.Public.class)
    @Id
    private String id;

    @JsonView(Views.Public.class)
    private Instant started;

    @JsonView(Views.Public.class)
    private Instant completed;

    @JsonView(Views.Public.class)
    private String lastMessage;

    @JsonView(Views.Public.class)
    private Status status;

    @JsonView(Views.Public.class)
    private int percentage;
    /**
     * URL to logfile.
     */
    private String logfile;

    public JobProgress(String id) {
        this.id = id;
    }

    public enum Status {
        /**
         * The job hasn't started running yet and it waiting to
         */
        QUEUED,
        /**
         * The job is currently running and we await the results.
         */
        PROCESSING,
        /**
         * The jobs completed successfully.
         */
        COMPLETED,
        /**
         * The job ran but there were errors.
         */
        PROBLEMS,
        /**
         * The job completely failed to run.
         */
        FAILED,
        /**
         * The jobs failed in an expected way. There is an exception here.
         */
        ERRORED
    }
}
