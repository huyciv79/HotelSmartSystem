package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "paymentsplits")
public class Paymentsplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "splitid", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "paymentid", nullable = false)
    private Payment paymentid;

    @NotNull
    @Column(name = "subamount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subamount;

    @Size(max = 50)
    @NotNull
    @Column(name = "submethod", nullable = false, length = 50)
    private String submethod;

    @Size(max = 100)
    @Column(name = "subtransactioncode", length = 100)
    private String subtransactioncode;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'Pending'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

}