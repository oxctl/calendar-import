package uk.ac.ox.it.calendarimporter.jobs.ical;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This maps a iCal calendar UUID to a Canvas ID. This isn't thread safe and doesn't enforce
 * consistency. It also assumes the canvas ID space never has a duplicate.
 */
public class CalendarIDLookupFile implements CalendarIDLookup {

  private Map<String, Set<Integer>> uuidToId = new HashMap<>();
  private Map<Integer, String> idToUuid = new HashMap<>();

  private String filename;

  public CalendarIDLookupFile(String filename) {
    this.filename = filename;
  }

  public void load() throws IOException {
    File file = new File(filename);
    Properties props = new Properties();
    try (Reader reader = new FileReader(file)) {
      props.load(reader);
      for (String uuid : props.stringPropertyNames()) {
        String value = props.getProperty(uuid);
        if (value != null) {
          try {
            String[] split = value.split(",");
            Set<Integer> ids = Arrays.stream(split).map(Integer::new).collect(Collectors.toSet());
            set(uuid, ids);
          } catch (NumberFormatException nfe) {
            throw new IOException("Failed to parse number for " + uuid + " of " + value);
          }
        }
      }
    } catch (FileNotFoundException fnfe) {
      // Ignore
    }
  }

  public void save() throws IOException {
    File file = new File(filename);
    Properties props = new Properties();
    for (Map.Entry<String, Set<Integer>> uuidToId : uuidToId.entrySet()) {
      props.put(
          uuidToId.getKey(),
          uuidToId.getValue().stream().map(Object::toString).collect(Collectors.joining(",")));
    }
    try (Writer writer = new FileWriter(file)) {
      props.store(writer, "ID Mapping for iCal to Canvas");
    }
  }

  @Override
  public Set<Integer> getCanvasIDs(String uuid) {
    return uuidToId.get(uuid);
  }

  @Override
  public String getICalUUID(Integer id) {
    return idToUuid.get(id);
  }

  @Override
  public void set(String uuid, Set<Integer> ids) {
    Set<Integer> copy = new HashSet<>(ids);
    uuidToId.put(uuid, Collections.unmodifiableSet(copy));
    for (Integer id : ids) {
      idToUuid.put(id, uuid);
    }
  }
}
