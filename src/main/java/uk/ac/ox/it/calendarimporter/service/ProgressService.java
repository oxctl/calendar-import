package uk.ac.ox.it.calendarimporter.service;

import com.google.common.base.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Tracks the progress of jobs running. At the moment all updates go through to the database which isn't ideal,
 * we could use an in-memory store in the future.
 */
@Component
@Slf4j
public class ProgressService {

    @Autowired
    private JobProgressRepository progressRepository;

    /**
     * This is designed to be used by a job reporting it's progress.
     *
     * @param triggerId  The trigger ID.
     * @param message    The latest message.
     * @param percentage The percentage complete the job is estimated to be.
     */
    @Transactional
    public JobProgress updateJob(String triggerId, String message, Integer percentage) {
        checkArgument(triggerId != null, "You must supply a triggerId");
        checkArgument(message != null, "You must supply a message");
        checkArgument(percentage != null && 0 <= percentage && percentage <= 100, "Percentage must be from 0 to 100");

        JobProgress progress = progressRepository.findById(triggerId)
                .orElseGet((Supplier<JobProgress>) () -> createJobProgress(triggerId));
        if (progress.getCompleted() != null) {
            throw new IllegalStateException("Can't update a job that's complete: " + triggerId);
        }
        progress.setLastMessage(message);
        progress.setStatus(JobProgress.Status.RUNNING);
        progress.setPercentage(percentage);
        return progressRepository.save(progress);
    }

    /**
     * Create a new JobProgress initialised at a basic level.
     *
     * @param triggerId The trigger id.
     * @return A new job progress that hasn't yet been persisted yet.
     */
    private static JobProgress createJobProgress(String triggerId) {
        JobProgress jobProgress = new JobProgress(triggerId);
        jobProgress.setStarted(Instant.now());
        return jobProgress;
    }

    /**
     * Used by listener to mark a job as running.
     *
     * @param triggerId The trigger ID.
     */
    @Transactional
    public JobProgress updateJobStarted(String triggerId) {
        log.debug("Trigger {} started", triggerId);
        JobProgress jobProgress = progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
        jobProgress.setStatus(JobProgress.Status.RUNNING);
        jobProgress.setStarted(Instant.now());
        return progressRepository.save(jobProgress);
    }

    /**
     * Used by listener to mark a job as finished/failed.
     *
     * @param triggerId The job ID.
     */
    @Transactional
    public JobProgress updateJobStopped(String triggerId, String error) {
        log.debug("Trigger {} stopped", triggerId);
        JobProgress jobProgress = progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
        if (error != null) {
            jobProgress.setStatus(JobProgress.Status.FAILED);
            jobProgress.setLastMessage(error);
        } else {
            jobProgress.setStatus(JobProgress.Status.COMPLETED);
        }
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
        JobProgress jobProgress = progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
        jobProgress.setStatus(JobProgress.Status.QUEUED);
        jobProgress.setLastMessage("Job queued");
        return progressRepository.save(jobProgress);
    }

    public Optional<JobProgress> findById(String triggerId) {
        // TODO Check the user owning the job
        return progressRepository.findById(triggerId);
    }

}
