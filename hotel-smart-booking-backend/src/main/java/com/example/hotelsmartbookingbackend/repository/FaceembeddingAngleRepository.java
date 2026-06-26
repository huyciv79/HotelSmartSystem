package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.FaceembeddingAngle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface FaceembeddingAngleRepository extends JpaRepository<FaceembeddingAngle, Integer> {

    @Modifying
    @Query(value = """
            DELETE FROM faceembedding_angles
            WHERE embeddingid = :embeddingId
            """, nativeQuery = true)
    void deleteByEmbeddingId(@Param("embeddingId") Integer embeddingId);

    @Modifying
    @Query(value = """
            INSERT INTO faceembedding_angles (
                embeddingid,
                pose,
                embedding,
                yaw_score,
                pitch_score,
                quality_score,
                liveness_score,
                available,
                fail_reason,
                createdat
            )
            VALUES (
                :embeddingId,
                :pose,
                CAST(:embedding AS vector),
                :yawScore,
                :pitchScore,
                :qualityScore,
                :livenessScore,
                :available,
                :failReason,
                :createdAt
            )
            ON CONFLICT (embeddingid, pose)
            DO UPDATE SET embedding      = CAST(EXCLUDED.embedding AS vector),
                          yaw_score      = EXCLUDED.yaw_score,
                          pitch_score    = EXCLUDED.pitch_score,
                          quality_score  = EXCLUDED.quality_score,
                          liveness_score = EXCLUDED.liveness_score,
                          available      = EXCLUDED.available,
                          fail_reason    = EXCLUDED.fail_reason,
                          createdat      = EXCLUDED.createdat
            """, nativeQuery = true)
    void upsertAngle(@Param("embeddingId") Integer embeddingId,
                     @Param("pose") String pose,
                     @Param("embedding") String embedding,
                     @Param("yawScore") Double yawScore,
                     @Param("pitchScore") Double pitchScore,
                     @Param("qualityScore") Double qualityScore,
                     @Param("livenessScore") Double livenessScore,
                     @Param("available") Boolean available,
                     @Param("failReason") String failReason,
                     @Param("createdAt") Instant createdAt);
}
