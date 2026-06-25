package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "faceembedding_angles")
public class FaceembeddingAngle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "angleid", nullable = false)
    private Integer id;

    @Column(name = "embeddingid", nullable = false)
    private Integer embeddingid;

    @Column(name = "pose", nullable = false)
    private String pose;

    @Column(name = "embedding", columnDefinition = "vector(512)")
    private String embedding;

    @Column(name = "yaw_score")
    private Double yawScore;

    @Column(name = "pitch_score")
    private Double pitchScore;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "liveness_score")
    private Double livenessScore;

    @Column(name = "available", nullable = false)
    private Boolean available;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "createdat")
    private Instant createdat;
}
