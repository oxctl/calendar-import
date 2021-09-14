package uk.ac.ox.it.calendarimporter.controller;

import org.quartz.Job;
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVImportJob;
import uk.ac.ox.it.calendarimporter.jobs.ical.IcalImportJob;

public enum ImportType {
    CSV(CSVImportJob.class),
    ICAL(IcalImportJob.class);

    private final Class<? extends Job> jobClass;

    ImportType(Class<? extends Job> jobClass) {
        this.jobClass = jobClass;
    }

    public Class<? extends Job> getJobClass() {
        return jobClass;
    }
}
