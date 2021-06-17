package uk.ac.ox.it.calendarimporter.persistence.model;

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

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import uk.ac.ox.it.calendarimporter.Views;

@Table(
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"tenant", "username"}),
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

  // The username from Canvas
  @NotNull
  @Column(name = "username", nullable = false)
  private String username;

  // The subject from Canvas
  @Column(name = "subject")
  private String subject;

  // The name of the Canvas tenant
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "tenant", nullable = false)
  private Tenant tenant;

  private String email;

  /** We store the locale so that the background jobs can send errors in the correct locale. */
  private String locale;

  /** The displayed name for the user. */
  @JsonView(Views.Public.class)
  private String name;

  public User() {}

  public User(Tenant tenant, String username) {
    this.tenant = tenant;
    this.username = username;
  }
}
