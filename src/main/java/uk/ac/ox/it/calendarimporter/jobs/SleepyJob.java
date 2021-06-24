package uk.ac.ox.it.calendarimporter.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Simple job for testing that just sleeps.
 */
public class SleepyJob implements Job {

    public static final int DEFAULT_TIME = 60 * 1000;

    @Override
    public void execute(JobExecutionContext context) {
        int time;
        try {
            time = context.getMergedJobDataMap().getInt("time");
        } catch (ClassCastException cce) {
            time = DEFAULT_TIME;
        }

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
