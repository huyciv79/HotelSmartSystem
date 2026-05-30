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
@Table(name = "feedbackimages")
public class Feedbackimage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedbackimageid", nullable = false)
    private Integer id;

    @Size(max = 500)
    @NotNull
    @Column(name = "imageurl", nullable = false, length = 500)
    private String imageurl;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "uploadedat", nullable = false)
    private Instant uploadedat;

}