package uk.ac.ox.it.calendarimporter.controller;

import org.quartz.Job;
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVImportJob;
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVReimportJob;
import uk.ac.ox.it.calendarimporter.jobs.ical.IcalImportJob;

public enum ImportType {
    CSV(CSVImportJob.class, false),

    /**
     * This CSV job is designed to run multiple times and keep the events in the calendar in-sync with the feed.
     */
    CSV_REIMPORT(CSVReimportJob.class, true),
    
    ICAL(IcalImportJob.class, false);

    private final Class<? extends Job> jobClass;
    
    // Should this import type be run repeatedly?
    private final boolean repeats;

    ImportType(Class<? extends Job> jobClass, boolean repeats) {
        this.jobClass = jobClass;
        this.repeats = repeats;
    }

    public Class<? extends Job> getJobClass() {
        return jobClass;
    }

    public boolean isRepeats() {
        return repeats;
    }
}
