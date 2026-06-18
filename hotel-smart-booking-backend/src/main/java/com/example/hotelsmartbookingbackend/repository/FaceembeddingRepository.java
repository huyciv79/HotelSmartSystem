package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Faceembedding;
import com.example.hotelsmartbookingbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface FaceembeddingRepository extends JpaRepository<Faceembedding, Integer> {

    /**
     * Tìm face embedding theo user.
     */
    Optional<Faceembedding> findByUserid(User user);

    /**
     * Upsert embedding: cập nhật nếu đã tồn tại cho user này.
     * Sử dụng native query với ON CONFLICT của PostgreSQL.
     *
     * Lưu ý: Phải dùng CAST(:embedding AS vector) hoặc :embedding::vector
     * vì PostgreSQL không tự động cấu trúc khi nhận String param cho cột vector.
     */
    @Modifying
    @Query(value = """
            INSERT INTO faceembeddings (userid, embedding, registeredat)
            VALUES (:userId, CAST(:embedding AS vector), :registeredAt)
            ON CONFLICT (userid)
            DO UPDATE SET embedding   = CAST(EXCLUDED.embedding AS vector),
                          registeredat = EXCLUDED.registeredat
            """, nativeQuery = true)
    void upsertEmbedding(@Param("userId") Integer userId,
                         @Param("embedding") String embedding,
                         @Param("registeredAt") Instant registeredAt);
}
