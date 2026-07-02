package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "userid", nullable = false)
    private User userid;


    @Size(max = 512)
    @Column(name = "idcardnumber", length = 512)
    private String idcardnumber;

    @Size(max = 64)
    @Column(name = "idcardnumber_hash", length = 64, unique = true)
    private String idcardnumberhash;

    @Size(max = 512)
    @Column(name = "fullname", length = 512)
    private String fullname;

    @Size(max = 512)
    @Column(name = "dateofbirth", length = 512)
    private String dateofbirth;

    @Size(max = 512)
    @Column(name = "gender", length = 512)
    private String gender;

    @Size(max = 512)
    @Column(name = "hometown", length = 512)
    private String hometown;

    @Size(max = 20)
    @Column(name = "provincecode", length = 20)
    private String provincecode;

    @Size(max = 512)
    @Column(name = "provincename", length = 512)
    private String provincename;

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
    @ColumnDefault("'SUBMITTED'")
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
