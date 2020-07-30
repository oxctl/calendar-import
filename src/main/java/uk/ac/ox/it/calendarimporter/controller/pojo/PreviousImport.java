package uk.ac.ox.it.calendarimporter.controller.pojo;

import static uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status.*;

import com.github.marlonlom.utilities.timeago.TimeAgo;
import lombok.Data;
import uk.ac.ox.it.calendarimporter.controller.ImportType;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;

/** This holds all the data needed to display a previous import in the UI. */
@Data
public class PreviousImport {

  String id;
  String contextJobId;
  String user;
  String when;
  String filename;
  String dest;
  boolean canDelete;
  String icon;
  Job load;
  Job delete;

  public PreviousImport(ContextJob job) {
    this(job.getCalendarImport());
    this.contextJobId = Long.toString(job.getId());
  }

  public PreviousImport(CalendarImport calendarImport) {
    this.id = Long.toString(calendarImport.getId());
    this.user = calendarImport.getUser().getName();

    this.when = TimeAgo.using(calendarImport.getCreated().toEpochMilli());
    this.filename = calendarImport.getFilename();
    this.canDelete = canDelete(calendarImport);
    if (calendarImport.getLoad() != null) {
      this.load = new Job(calendarImport.getLoad());
    }
    this.icon = (ImportType.CSV.equals(calendarImport.getType()))?"icon-ms-excel":"icon-calendar-month";
    this.dest = calendarImport.getDestinationName();
    if (calendarImport.getDelete() != null) {
      this.delete = new Job(calendarImport.getDelete());
    }
  }

  private static String toStatusString(JobProgress.Status status) {
    if (status != null) {
      String input = status.name();
      return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    } else {
      return "Unknown";
    }
  }

  private boolean canDelete(CalendarImport calendarImport) {
    if (calendarImport.getLoad() == null) {
      return false;
    }
    JobProgress.Status status = calendarImport.getLoad().getStatus();
    return (calendarImport.getDelete() == null
            || FAILED.equals(calendarImport.getDelete().getStatus()))
        && (COMPLETED.equals(status)
            || FAILED.equals(status)
            || ERRORED.equals(status)
            || PROBLEMS.equals(status));
  }

  @Data
  public class Job {

    String status;
    String message;
    Progress progress;
    boolean hasLog;
    String logId;
    boolean isComplete;
    boolean isFailed;
    boolean isProblem;

    public Job(JobProgress jobProgress) {
      this.status = toStatusString(jobProgress.getStatus());
      this.message = jobProgress.getLastMessage();
      if (PROCESSING.equals(jobProgress.getStatus())) {
        this.progress = new Progress(jobProgress.getPercentage());
      }
      hasLog = jobProgress.getLogfile() != null;
      this.logId = jobProgress.getId();
      this.isComplete = jobProgress.getStatus().equals(COMPLETED);
      this.isFailed = jobProgress.getStatus().equals(FAILED);
      this.isProblem = jobProgress.getStatus().equals(PROBLEMS);
    }

    @Data
    public class Progress {

      int complete;

      public Progress(int complete) {
        this.complete = complete;
      }
    }
  }
}
