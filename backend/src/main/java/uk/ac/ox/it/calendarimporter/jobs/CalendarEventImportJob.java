package uk.ac.ox.it.calendarimporter.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PersistJobDataAfterExecution
public class CalendarEventImportJob implements Job {

    private final Logger log = LoggerFactory.getLogger(CalendarEventImportJob.class);

    @Override
    public void execute(JobExecutionContext context) {

        // set up calendar syncing for user and calendarUrl

        // userID and calendarUrl are in the job data map

    }
}
