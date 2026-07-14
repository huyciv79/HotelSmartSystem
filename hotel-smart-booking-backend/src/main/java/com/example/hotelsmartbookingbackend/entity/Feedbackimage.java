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
public class FeedbackImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedbackimageid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @JoinColumn(name = "feedbackid", nullable = false)
    private Feedback feedback;

    @Size(max = 10485760)
    @NotNull
    @Column(name = "imageurl", nullable = false, length = 10485760)
    private String imageUrl;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "uploadedat", nullable = false)
    private Instant uploadedAt;
}