package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "roomtypeimages")
public class RoomTypeImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imageid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "roomtypeid", nullable = false)
    private RoomType roomType;

    @NotNull
    @Column(name = "imageurl", nullable = false, length = Integer.MAX_VALUE)
    private String imageUrl;

    @ColumnDefault("false")
    @Column(name = "isprimary")
    private Boolean isPrimary;

    @ColumnDefault("0")
    @Column(name = "displayorder")
    private Integer displayOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "createdat")
    private Instant createdAt;
}