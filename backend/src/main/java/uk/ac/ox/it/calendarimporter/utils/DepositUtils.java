package uk.ac.ox.it.calendarimporter.utils;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.ac.ox.it.calendarimporter.service.DepositService.Type;

@Component
public class DepositUtils {


    private static DateTimeFormatter formatter;


    @Value("${calendar.upload.format:yyyy-MM}")
    public void setFormatPattern(String formatPattern) {
        formatter = DateTimeFormatter.ofPattern(formatPattern);
    } 

    public Path resolveTargetPath(Path root, File file, Type type) {
        return root
                .resolve(type.getDirectory())
                .resolve(formatter.format(LocalDateTime.now()))
                .resolve(UUID.randomUUID().toString())
                .resolve(file.getName());
    }
}
