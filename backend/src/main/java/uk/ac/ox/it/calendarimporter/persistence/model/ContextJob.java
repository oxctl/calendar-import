package uk.ac.ox.it.calendarimporter.persistence.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.ac.ox.it.calendarimporter.Views;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToOne;
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
