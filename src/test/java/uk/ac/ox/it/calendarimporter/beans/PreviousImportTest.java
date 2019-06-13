package uk.ac.ox.it.calendarimporter.beans;

import org.junit.Test;
import uk.ac.ox.it.calendarimporter.controller.pojo.PreviousImport;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

import java.time.Instant;

public class PreviousImportTest {

    @Test
    public void testNoLoad() {
        // This is when there isn't a load job for some reason.
        User user = new User();
        user.setName("Test User");
        Instant now = Instant.now();

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setUser(user);
        calendarImport.setCreated(now);
        new PreviousImport(calendarImport);
    }
}
