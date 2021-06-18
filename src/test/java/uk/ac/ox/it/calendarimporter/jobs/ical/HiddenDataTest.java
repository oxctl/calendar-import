package uk.ac.ox.it.calendarimporter.jobs.ical;

import org.junit.jupiter.api.Test;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

import static org.junit.jupiter.api.Assertions.*;

public class HiddenDataTest {

    @Test
    public void testRoundTrip() {
        String encoded = HiddenData.toHidden("1234");
        String devoded = HiddenData.fromHidden(encoded);
        assertEquals("1234", devoded);
    }

    @Test
    public void testInvalid() {
        assertThrows(IllegalArgumentException.class, () -> HiddenData.fromHidden(
                "<div class=\"calendar-data-1\" style=\"display: none;\" data-calendar=\"  \"></div>"));
    }

    @Test
    public void testOtherComment() {
        assertNull(HiddenData.fromHidden("<div>Hello World</div>"));
    }
}
