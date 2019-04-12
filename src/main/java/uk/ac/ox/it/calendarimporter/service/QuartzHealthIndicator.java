package uk.ac.ox.it.calendarimporter.service;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Simple quartz scheduler health indicator
 */
@Component
public class QuartzHealthIndicator implements HealthIndicator {

    @Autowired
    private Scheduler scheduler;

    @Override
    public Health health() {
        Health.Builder builder  ;
        try {
            if (scheduler.isStarted()) {
                builder = Health.up();
            } else {
                builder = Health.outOfService();
            }
            builder.withDetail("jobs", scheduler.getMetaData().getNumberOfJobsExecuted());
            builder.withDetail("instance", scheduler.getMetaData().getSchedulerInstanceId());
            builder.withDetail("standby", scheduler.isInStandbyMode());
        } catch (SchedulerException e) {
            builder = Health.down(e);
        }
        return builder.build();
    }
}
