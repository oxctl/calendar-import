package uk.ac.ox.it.calendarimporter.controller.pojo;

import com.github.marlonlom.utilities.timeago.TimeAgo;
import lombok.Data;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;

import static uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status.*;

@Data
public class PreviousImport {

    String id;
    String user;
    String when;
    String filename;
    boolean canDelete;
    Job load;
    Job delete;

    public PreviousImport(CalendarImport calendarImport) {
        this.id = Long.toString(calendarImport.getId());
        this.user = calendarImport.getUser().getName();

        this.when = TimeAgo.using(calendarImport.getCreated().toEpochMilli());
        this.filename = calendarImport.getFilename();
        this.canDelete = canDelete(calendarImport);
        this.load = new Job(calendarImport.getLoad());
        if (calendarImport.getDelete() != null) {
            this.delete = new Job(calendarImport.getDelete());
        }
    }

    boolean canDelete(CalendarImport calendarImport) {
        JobProgress.Status status = calendarImport.getLoad().getStatus();
        return (calendarImport.getDelete() == null || FAILED.equals(calendarImport.getDelete().getStatus()))
                && (COMPLETED.equals(status) || FAILED.equals(status));
    }

    @Data
    class Job {

        String status;
        String message;
        Progress progress;

        public Job(JobProgress jobProgress) {
            this.status = toStatusString(jobProgress.getStatus());
            this.message = jobProgress.getLastMessage();
            if (RUNNING.equals(jobProgress.getStatus())) {
                this.progress = new Progress(jobProgress.getPercentage());
            }
        }

        @Data
        class Progress {
            int complete;

            public Progress(int complete) {
                this.complete = complete;
            }
        }
    }

    public static String toStatusString(JobProgress.Status status) {
        if (status != null) {
            String input = status.name();
            return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
        } else {
            return "Unknown";
        }
    }

}
