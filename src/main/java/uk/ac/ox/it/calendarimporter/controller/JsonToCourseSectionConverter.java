package uk.ac.ox.it.calendarimporter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.controller.pojo.CourseSection;

/** This is used to convert a JSON string back to a course section */
@Component
class JsonToCourseSectionConverter implements Converter<String, CourseSection> {

  private final ObjectMapper jsonMapper = new ObjectMapper();

  public CourseSection convert(String source) {
    if (source.isBlank()) {
      return null;
    }
    try {
      return jsonMapper.readValue(source, CourseSection.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to convert: " + source, e);
    }
  }
}
