package uk.ac.ox.it.calendarimporter.persistence.model;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import uk.ac.ox.it.calendarimporter.Views;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * This links an CalendarImport to a context that it's run against.
 * When returning this object through the API it normally wants to use a custom JSON view to prevent
 * secret fields from being displayed.
 */
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
    @JsonView(Views.Public.class)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @NotNull
    @ManyToOne(optional = false)
    private Tenant tenant;

    @NotNull
    private String context;

    // We need to be able to sort the jobs in the DB.
    @JsonView(Views.Public.class)
    @NotNull
    private Instant created;

    /**
     * Should this import be hidden from the UI.
     */
    private boolean hidden;

    @NotNull
    @OneToOne(optional = false)
    @JsonView(Views.Public.class)
    private CalendarImport calendarImport;
}
