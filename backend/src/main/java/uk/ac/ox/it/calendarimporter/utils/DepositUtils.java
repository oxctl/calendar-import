package uk.ac.ox.it.calendarimporter.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.service.DepositService.Type;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class DepositUtils {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");;
    
    private Clock clock = Clock.systemUTC();

    @Value("${calendar.upload.format:yyyy-MM}")
    public void setFormatPattern(String formatPattern) {
        formatter = DateTimeFormatter.ofPattern(formatPattern);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Builds the parts of the upload path.
     * This doesn't join the parts together as different implementations may need to concatenate them differently.
     * @param filename The name of the file.
     * @param type The type of the upload.
     */
    public String[] resolveTargetPath(String filename, Type type) {
        return new String[]{
                type.getDirectory(),
                formatter.format(clock.instant().atZone(ZoneId.of("UTC"))),
                UUID.randomUUID().toString(),
                filename
        };
    }
}
