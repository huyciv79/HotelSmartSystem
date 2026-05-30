package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ekyc_profiles")
public class EkycProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ekycid", nullable = false)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Column(name = "idcardnumber", nullable = false, length = 50)
    private String idcardnumber;

    @Size(max = 512)
    @Column(name = "frontimage", length = 512)
    private String frontimage;

    @Size(max = 512)
    @Column(name = "backimage", length = 512)
    private String backimage;

    @Size(max = 512)
    @Column(name = "faceimage", length = 512)
    private String faceimage;

    @Column(name = "verifiedat")
    private Instant verifiedat;

    @Size(max = 500)
    @Column(name = "rejectionreason", length = 500)
    private String rejectionreason;

    @Size(max = 50)
    @Column(name = "verificationmethod", length = 50)
    private String verificationmethod;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Pending'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat", nullable = false)
    private Instant createdat;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updatedat", nullable = false)
    private Instant updatedat;

}