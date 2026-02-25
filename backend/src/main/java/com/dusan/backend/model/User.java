package com.dusan.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "username")})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Username is mandatory")
    @Email(message = "Username must be a valid email address")
    private String username;

    @Column
    @NotBlank(message = "Name is mandatory")
    private String name;

    @Column(length = 1000)
    private String permissionsString;

    @Transient
    private Set<String> permissions = new HashSet<>();


    @Column
    @NotBlank(message = "Password is mandatory")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer loginCount = 0;

    @Version
    private Integer version = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer balance = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer salary = 0;


    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isAdmin = false;

    public Set<String> getPermissions() {
        if (permissionsString != null && !permissionsString.isEmpty()) {
            return Arrays.stream(permissionsString.split(","))
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
        this.permissionsString = String.join(",", permissions);
    }

}
