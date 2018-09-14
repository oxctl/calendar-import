package uk.ac.ox.it.calendarimporter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Component
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
    public void updateJob(String triggerId, String message, Integer percentage) {
        checkArgument(triggerId != null, "You must supply a triggerId");
        checkArgument(message != null, "You must supply a message");
        checkArgument(percentage != null && 0 <= percentage && percentage <= 100, "Percentage must be from 0 to 100");

        JobProgress progress = progressRepository.findById(triggerId).orElse(JobProgress.builder().id(triggerId).started(Instant.now()).build());
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
        progressRepository.save(progress);
    }

    /**
     * Used by listener to mark a job as running.
     * @param triggerId The trigger ID.
     */
    @Transactional
    public void updateJobStarted(String triggerId) {
        JobProgress jobProgress = progressRepository.findById(triggerId).orElse(JobProgress.builder().id(triggerId).build());
        jobProgress.setStatus(JobProgress.Status.RUNNING);
        jobProgress.setStarted(Instant.now());
        progressRepository.save(jobProgress);
    }

    /**
     * Used by listener to mark a job as finished/failed.
     * @param jobId The job ID.
     */
    @Transactional
    public void updateJobStopped(String jobId, String error) {
        JobProgress jobProgress = progressRepository.findById(jobId).orElse(JobProgress.builder().id(jobId).build());
        if (error != null) {
            jobProgress.setStatus(JobProgress.Status.FAILED);
            jobProgress.setLastMessage(error);
        } else {
            jobProgress.setStatus(JobProgress.Status.COMPLETED);
        }
        jobProgress.setCompleted(Instant.now());
        progressRepository.save(jobProgress);
    }

    /**
     * Used by listener to mark a job as queued
     * @param jobId The job ID.
     */
    @Transactional
    public void updateJobCreated(String jobId ) {
        JobProgress jobProgress = progressRepository.findById(jobId).orElse(JobProgress.builder().id(jobId).build());
        jobProgress.setStatus(JobProgress.Status.QUEUED);
        jobProgress.setCompleted(Instant.now());
        progressRepository.save(jobProgress);
    }

    public Optional<JobProgress> findById(String jobId) {
        // TODO Check the user owning the job
        return progressRepository.findById(jobId);
    }

}
