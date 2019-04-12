package uk.ac.ox.it.calendarimporter.persistence.model;

import java.time.Instant;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.Data;

/** This links an CalendarImport to a context that it's run against. */
@Entity
@Data
@NamedEntityGraphs({
  @NamedEntityGraph(
      name = "contextJobWithCalendarImport",
      attributeNodes = {@NamedAttributeNode(value = "calendarImport", subgraph = "all")},
      subgraphs = {
        @NamedSubgraph(
            name = "all",
            attributeNodes = {
              @NamedAttributeNode("user"),
              @NamedAttributeNode("load"),
              @NamedAttributeNode("delete")
            })
      })
})
public class ContextJob {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private long id;

  @NotNull
  @ManyToOne(optional = false)
  private Tenant tenant;

  @NotNull private String context;

  // We need to be able to sort the jobs in the DB.
  @NotNull private Instant created;

  /** Should this import be hidden from the UI. */
  private boolean hidden;

  @NotNull
  @OneToOne(optional = false)
  private CalendarImport calendarImport;
}
