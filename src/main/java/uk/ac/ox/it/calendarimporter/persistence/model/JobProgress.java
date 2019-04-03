package uk.ac.ox.it.calendarimporter.persistence.model;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds the progress of a job that means that we don't have to query quartz to find out what's
 * happened. Job progress has an idea of how "complete" a job is.
 */
@Data
@Entity
@NoArgsConstructor
public class JobProgress {

  /** This is the trigger ID. */
  @Id private String id;

  private Instant started;
  private Instant completed;
  private String lastMessage;
  private Status status;
  private int percentage;
  /** URL to logfile. */
  private String logfile;

  public JobProgress(String id) {
    this.id = id;
  }

  public enum Status {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
  }
}
