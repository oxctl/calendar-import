package uk.ac.ox.it.calendarimporter.jobs.ical;

import org.junit.Assert;
import org.junit.Test;

public class HiddenDataTest {

    @Test
    public void testRoundTrip() {
        String encoded = HiddenData.toHidden("1234");
        String devoded = HiddenData.fromHidden(encoded);
        Assert.assertEquals("1234", devoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() {
        HiddenData.fromHidden("<div class=\"calendar-data-1\" style=\"display: none;\" data-calendar=\"  \"></div>");
    }

    @Test
    public void testOtherComment() {
        Assert.assertNull(HiddenData.fromHidden("<div>Hello World</div>"));
    }
}
