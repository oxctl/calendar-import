package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UtilsTest {

    @Test
    public void testToImportTypeContentTypeICal(){
        MockMultipartFile file = createFile("text/calendar");
        ImportType importType = Utils.toImportType(file);
        assertEquals(ImportType.ICAL, importType);
    }

    @Test
    public void testToImportContentTypeTypeCSV(){
        MockMultipartFile file = createFile("text/csv");
        ImportType importType = Utils.toImportType(file);
        assertEquals(ImportType.CSV, importType);
    }

    @Test
    public void testToImportTypeNull(){
        MockMultipartFile file = createFile(null);
        ImportType importType = Utils.toImportType(file);
        assertNull(importType);
    }

    @Test
    public void testToImportTypeCSV(){
        MockMultipartFile file = createFile("application/json", ".csv");
        ImportType importType = Utils.toImportType(file);
        assertEquals(ImportType.CSV, importType);
    }

    @Test
    public void testToImportTypeICal(){
        MockMultipartFile file = createFile("application/json", ".ical");
        ImportType importType = Utils.toImportType(file);
        assertEquals(ImportType.ICAL, importType);
    }

    @Test
    public void testToImportTypeICS(){
        MockMultipartFile file = createFile("application/json", ".ics");
        ImportType importType = Utils.toImportType(file);
        assertEquals(ImportType.ICAL, importType);
    }

    @Test
    public void testToImportTypeICalendar(){
        MockMultipartFile file = createFile("application/json", ".icalendar");
        ImportType importType = Utils.toImportType(file);
        assertEquals(ImportType.ICAL, importType);
    }

    private MockMultipartFile createFile(String contentType){
        return createFile(contentType, "originalFilename");
    }

    private MockMultipartFile createFile(String contentType, String originalFilename){
        String file = "file";
        byte[] content = "content".getBytes();
        return new MockMultipartFile(file, originalFilename, contentType, content);
    }
}
