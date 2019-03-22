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
import lombok.Data;
import lombok.EqualsAndHashCode;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"tenant", "username"}))
@Entity
@Data
@EqualsAndHashCode
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  // The username from Canvas
  @NotNull
  @Column(name = "username", nullable = false)
  private String username;

  // The name of the Canvas tenant
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "tenant", nullable = false)
  private Tenant tenant;

  private String email;

  /** We store the locale so that the background jobs can send errors in the correct locale. */
  private String locale;

  /** The displayed name for the user. */
  private String name;

  public User() {}

  public User(Tenant tenant, String username) {
    this.tenant = tenant;
    this.username = username;
  }
}
