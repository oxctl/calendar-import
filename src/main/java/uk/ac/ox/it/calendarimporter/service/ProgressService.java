package uk.ac.ox.it.calendarimporter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Component
@Slf4j
public class ProgressService {

    @Autowired
    private JobProgressRepository progressRepository;

    /**
     * This is designed to be used by a job reporting it's progress.
     * @param triggerId
     * @param message
     * @param percentage
     */
    @Transactional
    public JobProgress updateJob(String triggerId, String message, Integer percentage) {
        checkArgument(triggerId != null, "You must supply a triggerId");
        checkArgument(message != null, "You must supply a message");
        checkArgument(percentage != null && 0 <= percentage && percentage <= 100, "Percentage must be from 0 to 100");

        JobProgress progress = progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
        if (progress.getCompleted() != null) {
            throw new IllegalStateException("Can't update a job that's complete: " + triggerId);
        }
        progress.setLastMessage(message);
        if (percentage != null) {
            progress.setStatus(JobProgress.Status.RUNNING);
            progress.setPercentage(percentage);
            if (percentage == 100) {
                progress.setCompleted(Instant.now());
                progress.setStatus(JobProgress.Status.COMPLETED);
            }
        }
        return progressRepository.save(progress);
    }

    private JobProgress createJobProgress(String id) {
        JobProgress jobProgress = new JobProgress(id);
        jobProgress.setStarted(Instant.now());
        return jobProgress;
    }

    /**
     * Used by listener to mark a job as running.
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
     * @param triggerId The job ID.
     */
    @Transactional
    public JobProgress updateJobCreated(String triggerId ) {
        log.debug("Trigger {} created", triggerId);
        JobProgress jobProgress = progressRepository.findById(triggerId).orElse(createJobProgress(triggerId));
        jobProgress.setStatus(JobProgress.Status.QUEUED);
        jobProgress.setCompleted(Instant.now());
        return progressRepository.save(jobProgress);
    }

    public Optional<JobProgress> findById(String triggerId) {
        // TODO Check the user owning the job
        return progressRepository.findById(triggerId);
    }

}
