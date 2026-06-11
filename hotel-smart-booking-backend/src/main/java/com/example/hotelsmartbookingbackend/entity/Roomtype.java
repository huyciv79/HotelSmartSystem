package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "roomtypes")
public class Roomtype {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomtypeid", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @Column(name = "baseprice", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseprice;

    @NotNull
    @ColumnDefault("2")
    @Column(name = "adultcapacity", nullable = false)
    private Integer adultcapacity;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "childcapacity", nullable = false)
    private Integer childcapacity;

    @Column(name = "totalcapacity", insertable = false, updatable = false)
    private Integer totalcapacity;

    @Column(name = "area", precision = 8, scale = 2)
    private BigDecimal area;

    @Size(max = 100)
    @Column(name = "bedtype", length = 100)
    private String bedtype;

    @Column(name = "amenities", length = Integer.MAX_VALUE)
    private String amenities;

    @Column(name = "images", length = Integer.MAX_VALUE)
    private String images;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Active'")
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
