package com.example.hotelsmartbookingbackend.entity;

import com.example.hotelsmartbookingbackend.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid", nullable = false)
    private Integer id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Size(max = 20)
    @NotNull
    @Column(name = "phonenumber", nullable = false, length = 20, unique = true)
    private String phoneNumber;

    @Size(min = 12, max = 12)
    @Pattern(regexp = "^[0-9]{12}$")
    @Column(name = "idcardnumber", length = 12, unique = true)
    private String idCardNumber;

    @Size(max = 255)
    @NotNull
    @Column(name = "passwordhash", nullable = false)
    private String passwordHash;

    @Size(max = 255)
    @NotNull
    @Column(name = "fullname", nullable = false)
    private String fullName;

    @Size(max = 512)
    @Column(name = "avatar", length = 512)
    private String avatar;

    @Column(name = "address", length = Integer.MAX_VALUE)
    private String address;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdAt;

    @Column(name = "updatedat")
    private Instant updatedAt;

}
