package uk.ac.ox.it.calendarimporter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest()
@DirtiesContext
public class CalendarImporterForCanvasApplicationTests {

    @Test
    public void contextLoads() {
        // Just check that the application starts up.
    }
}
