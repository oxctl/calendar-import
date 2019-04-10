package uk.ac.ox.it.calendarimporter.persistence.model;

import java.time.Instant;
import javax.persistence.*;
import lombok.Data;
import uk.ac.ox.it.calendarimporter.controller.ImportType;

/**
 * This holds details of an import that has been made. From here you can find the jobs related to
 * the initial import and possibly to the removal job.
 */
@Entity
@Data
public class CalendarImport {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  private String context;

  /** The user who created this import. */
  @ManyToOne private User user;
  /** When this was created. */
  private Instant created;
  /** The URL of the import. */
  private String url;

  /** The name of the course/section into which this import was done. */
  private String destinationName;
  /** The ID the of the course/section into which this import was done. */
  private String destinationId;

  /** The filename of the file uploaded */
  private String filename;

  /** The type of importer used. */
  private ImportType type;

  /** The job used to load the data. */
  @OneToOne private JobProgress load;

  /** The job used to remove the data. */
  @OneToOne private JobProgress delete;
}
