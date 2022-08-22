package uk.ac.ox.it.calendarimporter.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

/**
 * This is a quartz job that records any log messages that are written out.
 */
@Slf4j
public abstract class LoggingJob implements Job {

    private final Duration updateInterval = Duration.ofSeconds(1);

    @Autowired
    private DepositService depositService;

    @Autowired
    private ProgressService progressService;

    // These are for preventing lots of small DB updates.
    private Instant lastUpdate = Instant.MIN;
    private String unsavedMessage;
    private OutputStreamWriter logWriter;
    private String triggerId;
    private JobResult result;

    @Override
    public final void execute(JobExecutionContext context) throws JobExecutionException {
        triggerId = context.getTrigger().getKey().getName();
        result = new JobResult();
        File logfile;
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
            throw new JobExecutionException("Failed to open logfile for writing: " + logfile);
        } catch (Exception e) {
            log.warn("Failed to run job.", e);
            throw e;
        } finally {
            // Make sure to flush the last message.
            if (unsavedMessage != null) {
                progressService.updateJob(triggerId, unsavedMessage, null);
            }
            // Persist the log, as this is done after the job has completed, if a job is interrupted the
            // log is lost
            // and a recovery run will be in the logs instead.
            URL deposit;
            try {
                deposit = depositService.deposit(logfile, DepositService.Type.LOG);
                result.logfile = deposit.toExternalForm();
                context.setResult(result);
            } catch (IOException e) {
                log.error("Failed to save logfile.", e);
            }
        }
    }

    public abstract void executeLogged(JobExecutionContext context) throws JobExecutionException;

    public void log(String message, Object... args) {
        log(null, message, args);
    }

    public void log(Progress progress, String message, Object... args) {
        String formatted = (args.length > 0) ? String.format(message, args) : message;
        try {
            logWriter.append(Instant.now().toString()).append(" - ").append(formatted).append('\n');
        } catch (IOException e) {
            log.warn("Failed to write to logger {}", logWriter);
        }
        // Rate limit to updating the DB every 10 seconds.
        Instant now = Instant.now();
        if (lastUpdate.plus(updateInterval).isBefore(now)) {
            unsavedMessage = null;
            progressService.updateJob(
                    triggerId, formatted, (progress == null) ? null : progress.percent());
        } else {
            unsavedMessage = formatted;
        }
        lastUpdate = now;
    }

    public void failure(String message, Object... args) {
        log(message, args);
        result.failure = true;
    }

    public void problem(String message, Object... args) {
        log(message, args);
        result.problems = true;
    }
    
    public void reset() {
        String logfile = progressService.resetJob(triggerId);
        if (logfile != null) {
            depositService.remove(logfile);
        }
    }

    /**
     * Simple object to pass back the results of the Job.
     */
    public static class JobResult {
        private String logfile;
        private boolean problems;
        private boolean failure;

        public String getLogfile() {
            return logfile;
        }

        public boolean isProblems() {
            return problems;
        }

        public boolean isFailure() {
            return failure;
        }
    }

    /**
     * Simple object to track progress.
     */
    public static class Progress {

        private final int total;
        private int seen;

        /**
         * @param total The total number of items being counted.
         */
        public Progress(int total) {
            if (total < 0) {
                throw new IllegalArgumentException("total must be positive");
            }
            this.total = total;
            this.seen = 0;
        }

        /**
         * Called to flag we've processes another item. It doesn't ever progress past total.
         */
        public void increment() {
            if (seen < total) {
                seen++;
            } else {
                // Good code shouldn't ever attempt to over-increment.
                assert false;
            }
        }

        public int percent() {
            return 100 * seen / total;
        }

        public int getTotal() {
            return this.total;
        }

        public int getSeen() {
            return this.seen;
        }
    }
}
