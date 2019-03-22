package uk.ac.ox.it.calendarimporter.beans;

import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;

/** This merges together the UserJob and JobProgress */
public class UserJobProgress {

  private final UserJob userJob;
  private final JobProgress progress;

  public UserJobProgress(UserJob userJob, JobProgress progress) {
    this.userJob = userJob;
    this.progress = progress;
  }

  public Status getStatus() {
    return progress.getStatus();
  }
}
