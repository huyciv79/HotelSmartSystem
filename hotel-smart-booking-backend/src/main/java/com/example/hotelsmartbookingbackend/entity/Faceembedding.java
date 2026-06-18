package com.example.hotelsmartbookingbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Entity ánh xạ bảng faceembeddings.
 * Lưu trữ vector embedding 512 chiều dưới dạng chuỗi định dạng vector của PostgreSQL.
 *
 * Lưu ý: Sử dụng columnDefinition = "text" để lưu chuỗi "[0.1, -0.2, ...]"
 * tương thích với pgvector khi cần truy vấn sau này.
 */
@Getter
@Setter
@Entity
@Table(name = "faceembeddings")
public class Faceembedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "embeddingid", nullable = false)
    private Integer id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "userid", nullable = false)
    private User userid;

    /**
     * Vector embedding 512 chiều, kiểu VECTOR(512) của pgvector extension.
     * Hibernate lưu/đọc dưới dạng String theo định dạng "[0.12, -0.43, 0.78, ...]"
     * PostgreSQL JDBC driver tự động chuyển đổi chuỗi sang kiểu vector khi insert/update.
     */
    @Column(name = "embedding", columnDefinition = "vector(512)")
    private String embedding;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "registeredat")
    private Instant registeredat;
}