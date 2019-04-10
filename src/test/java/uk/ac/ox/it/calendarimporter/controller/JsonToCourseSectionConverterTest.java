package uk.ac.ox.it.calendarimporter.controller;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ox.it.calendarimporter.controller.pojo.CourseSection;

public class JsonToCourseSectionConverterTest {

  private JsonToCourseSectionConverter converter;

  @Before
  public void setUp() {
    converter = new JsonToCourseSectionConverter();
  }

  @Test
  public void testEmpty() {
    assertNull(converter.convert(""));
  }

  @Test
  public void testBlank() {
    assertNull(converter.convert("    "));
  }

  @Test
  public void testJson() {
    CourseSection section =
        converter.convert("{`sectionId`: `ID`, `name`: `Section Name`}".replace('`', '"'));
    assertNotNull(section);
    assertEquals("ID", section.getSectionId());
    assertEquals("Section Name", section.getName());
  }

  @Test
  public void testEmptyJson() {
    CourseSection section = converter.convert("{}");
    assertNotNull(section);
    assertNull(section.getSectionId());
    assertNull(section.getName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidJson() {
    converter.convert("{`otherId`: `ID`, `otherName`: `Section Name`}".replace('`', '"'));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotJson() {
    converter.convert("ID");
  }
}
