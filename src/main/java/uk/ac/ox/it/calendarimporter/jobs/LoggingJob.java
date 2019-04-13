package uk.ac.ox.it.calendarimporter.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

/**
 * This is a quartz job that records any log messages that are written out.
 */
@Slf4j
public abstract class LoggingJob implements Job {

    @Autowired
    private DepositService depositService;
    @Autowired
    private ProgressService progressService;

    // These are for preventing lots of small DB updates.
    private Instant lastUpdate = Instant.MIN;
    private String unsavedMessage;
    private final Duration updateInterval = Duration.ofSeconds(1);
    private OutputStreamWriter logWriter;
    private String triggerId;

    @Override
    public final void execute(JobExecutionContext context) throws JobExecutionException {
        triggerId = context.getTrigger().getKey().getName();
        File logfile = null;
        try {
            String jobKey = context.getJobDetail().getKey().toString();
            logfile = File.createTempFile(jobKey, ".log");
        } catch (IOException e) {
            throw new JobExecutionException("Failed to create logfile.", e);
        }
        try (OutputStreamWriter logWriter = new OutputStreamWriter(new FileOutputStream(logfile))) {
            this.logWriter = logWriter;
            executeLogged(context);
        } catch (IOException e) {
            throw new JobExecutionException("Failed to open logfile for writing: "+ logfile);
        } finally {
            // Make sure to flush the last message.
            if (unsavedMessage != null) {
                progressService.updateJob(triggerId, unsavedMessage, null);
            }
            // Persist the log, as this is done after the job has completed, if a job is interrupted the log is lost
            // and a recovery run will be in the logs instead.
            URL deposit = null;
            try {
                deposit = depositService.deposit(logfile, DepositService.Type.LOG);
            } catch (IOException e) {
                throw new JobExecutionException("Failed to save logfile.", e);
            }
            context.setResult(deposit.toExternalForm());
        }
    }

    public abstract void executeLogged(JobExecutionContext context) throws JobExecutionException;

    public void log(String message, Object... args) {
        log(null, message, args);
    }

    public void log(Float percent, String message, Object... args) {
        String formatted = (args.length > 0) ? String.format(message, args) : message;
        try {
            logWriter.append(formatted).append('\n');
        } catch (IOException e) {
            log.warn("Failed to write to logger {}", logWriter);
        }
        // Rate limit to updating the DB every 10 seconds.
        Instant now = Instant.now();
        if (lastUpdate.plus(updateInterval).isBefore(now)) {
            unsavedMessage = null;
            Integer percentInt = (percent != null)?percent.intValue():null;
            progressService.updateJob(triggerId, formatted, percentInt);
        } else {
            unsavedMessage = formatted;
        }
        lastUpdate = now;
    }
}
