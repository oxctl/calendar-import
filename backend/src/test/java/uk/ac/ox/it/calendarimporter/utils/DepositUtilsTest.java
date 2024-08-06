package uk.ac.ox.it.calendarimporter.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ox.it.calendarimporter.service.DepositService;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepositUtilsTest {

    private DepositUtils depositUtils;

    @BeforeEach
    public void setUp() {
        depositUtils = new DepositUtils();
        depositUtils.setClock(Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneId.of("UTC")));
    }

    @Test
    public void testSimple() throws IOException {
        assertThat(
                depositUtils.resolveTargetPath("test", DepositService.Type.LOG),
                // We don't care to match the UUID
                array(equalTo("logs"), equalTo("2000-01"), any(String.class), equalTo("test"))
        );
    }

    @Test
    public void testNull() throws IOException {
        // This shouldn't happen, but it's good to know it will.
        assertThrows(
                NullPointerException.class,
                () -> depositUtils.resolveTargetPath(null, null)
        );
    }

}