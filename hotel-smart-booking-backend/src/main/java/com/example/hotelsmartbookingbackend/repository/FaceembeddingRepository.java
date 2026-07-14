package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, Integer> {

    @Query(
            value = """
                    SELECT embedding::text
                    FROM faceembeddings
                    WHERE userid = :userId
                    """,
            nativeQuery = true
    )
    Optional<String> findEmbeddingTextByUserId(@Param("userId") Integer userId);

    @Query(
            value = """
                    SELECT embeddingid
                    FROM faceembeddings
                    WHERE userid = :userId
                    """,
            nativeQuery = true
    )
    Optional<Integer> findEmbeddingIdByUserId(@Param("userId") Integer userId);


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
