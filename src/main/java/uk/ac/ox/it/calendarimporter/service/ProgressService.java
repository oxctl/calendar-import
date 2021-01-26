package uk.ac.ox.it.calendarimporter.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.time.Instant;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;

/**
 * Tracks the progress of jobs running. At the moment all updates go through to the database which
 * isn't ideal, we could use an in-memory store in the future. The job does however limit the writes
 * through to the progress service so that it doesn't abuse the service with updates too frequently.
 */
@Service
@Slf4j
public class ProgressService {

  @Autowired private JobProgressRepository progressRepository;

  /**
   * Create a new JobProgress initialised at a basic level.
   *
   * @param triggerId The trigger id.
   * @return A new job progress that hasn't yet been persisted yet.
   */
  private JobProgress createJobProgress(String triggerId) {
    JobProgress jobProgress = new JobProgress(triggerId);
    jobProgress.setStarted(Instant.now());
    return jobProgress;
  }

  /**
   * This is designed to be used by a job reporting its progress.
   *
   * @param triggerId The trigger ID.
   * @param message The latest message.
   * @param percentage The percentage complete the job is estimated to be, can be null.
   */
  @Transactional
  public JobProgress updateJob(String triggerId, String message, Integer percentage) {
    checkArgument(triggerId != null, "You must supply a triggerId");
    checkArgument(message != null, "You must supply a message");
    checkArgument(
        percentage == null || 0 <= percentage && percentage <= 100,
        "Percentage must be from 0 to 100");

    JobProgress progress =
        progressRepository.findById(triggerId).orElseGet(() -> createJobProgress(triggerId));
    if (progress.getCompleted() != null) {
      throw new IllegalStateException("Can't update a job that's complete: " + triggerId);
    }
    progress.setLastMessage(message);
    progress.setStatus(JobProgress.Status.PROCESSING);
    if (percentage != null) {
      progress.setPercentage(percentage);
    }
    return progressRepository.save(progress);
  }

  /**
   * Used by listener to mark a job as running.
   *
   * @param triggerId The trigger ID.
   */
  @Transactional
  public JobProgress updateJobStarted(String triggerId) {
    log.debug("Trigger {} started", triggerId);
    JobProgress jobProgress =
        progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
    jobProgress.setStatus(JobProgress.Status.PROCESSING);
    jobProgress.setStarted(Instant.now());
    return progressRepository.save(jobProgress);
  }

  /**
   * Used by listener to mark a job as finished/failed.
   *
   * @param triggerId The job ID.
   */
  @Transactional
  public JobProgress updateJobStopped(String triggerId, String error, String logfile) {
    log.debug("Trigger {} stopped", triggerId);
    JobProgress jobProgress =
        progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
    if (error != null) {
      jobProgress.setStatus(JobProgress.Status.ERRORED);
      jobProgress.setLastMessage(error);
    } else {
      jobProgress.setStatus(JobProgress.Status.COMPLETED);
    }
    jobProgress.setLogfile(logfile);
    jobProgress.setCompleted(Instant.now());
    return progressRepository.save(jobProgress);
  }

  /**
   * Used by listener to mark a job as finished/failed.
   *
   * @param triggerId The job ID.
   */
  @Transactional
  public JobProgress updateJobStopped(
      String triggerId, String logfile, boolean problems, boolean failure) {
    log.debug("Trigger {} stopped", triggerId);
    JobProgress jobProgress =
        progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
    if (failure) {
      jobProgress.setStatus(JobProgress.Status.FAILED);
    } else if (problems) {
      jobProgress.setStatus(JobProgress.Status.PROBLEMS);
    } else {
      jobProgress.setStatus(JobProgress.Status.COMPLETED);
    }
    jobProgress.setLogfile(logfile);
    jobProgress.setCompleted(Instant.now());
    return progressRepository.save(jobProgress);
  }

  /**
   * Used by listener to mark a job as queued
   *
   * @param triggerId The job ID.
   */
  @Transactional
  public JobProgress updateJobCreated(String triggerId) {
    log.debug("Trigger {} created", triggerId);
    JobProgress jobProgress = findById(triggerId).orElseGet(() -> new JobProgress(triggerId));
    if (jobProgress.getStatus() != null) {
      log.warn("Job already exists for trigger {}, not setting to queued.", triggerId);
      return jobProgress;
    } else {
      jobProgress.setStatus(JobProgress.Status.QUEUED);
      jobProgress.setLastMessage("Job queued");
      return progressRepository.save(jobProgress);
    }
  }

  public Optional<JobProgress> findById(String triggerId) {
    // TODO Check the user owning the job
    return progressRepository.findById(triggerId);
  }
}
