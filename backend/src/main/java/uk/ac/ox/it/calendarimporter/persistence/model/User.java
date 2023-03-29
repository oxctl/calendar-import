package uk.ac.ox.it.calendarimporter.persistence.model;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import uk.ac.ox.it.calendarimporter.Views;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Table(
        name = "`user`",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant", "subject"})
        })
@Entity
@Data
@EqualsAndHashCode
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonView(Views.Public.class)
    private Long id;

    // The username from Canvas, we don't use this any more (since LTI 1.1 -> 1.3), but it's useful for debugging issues.
    // We now use the subject (from JWT) to identify the user.
    @NotNull
    @Column(name = "username", nullable = false)
    private String username;

    // The subject from Canvas
    @Column(name = "subject", nullable = false)
    private String subject;

    // The name of the Canvas tenant
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant", nullable = false)
    private Tenant tenant;

    private String email;

    /**
     * We store the locale so that the background jobs can send errors in the correct locale.
     */
    private String locale;

    /**
     * The displayed name for the user.
     */
    @JsonView(Views.Public.class)
    private String name;

    public User() {
    }

    public User(Tenant tenant, String subject, String username) {
        this.tenant = tenant;
        this.subject = subject;
        this.username = username;
    }
}
