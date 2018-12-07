package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_name", "username"})
)
@Entity
@Data
@EqualsAndHashCode
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // The username from Canvas
    @NotNull
    @Column(name="username", nullable = false)
    private String username;
    // The name of the Canvas tenant
    @NotNull
    @Column(name="tenant_name", nullable = false)
    private String tenantName;

    /**
     * This is the OAuth refresh token for the user.
     */
    private String token;

    private String email;

    /**
     * We store the locale so that the background jobs can send errors in the correct locale.
     */
    private String locale;

    public User() {
    }

    public User(String tenantName, String username) {
        this.tenantName = tenantName;
        this.username = username;
    }

}
