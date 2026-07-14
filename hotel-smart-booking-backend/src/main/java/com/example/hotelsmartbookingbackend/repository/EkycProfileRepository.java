package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.EkycProfile;
import com.example.hotelsmartbookingbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EkycProfileRepository extends JpaRepository<EkycProfile, Integer> {

    Optional<EkycProfile> findTopByUserOrderByCreatedAtDesc(User user);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EkycProfile e
            WHERE e.user = :user
              AND UPPER(e.status) = 'VERIFIED'
            """)
    boolean existsVerifiedByUserid(@Param("user") User user);

    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.status = 'AI_CHECKING',
                e.updatedAt = :updatedAt
            WHERE e.id = :ekycId
            """)
    void markAsAiChecking(@Param("ekycId") Integer ekycId,
                          @Param("updatedAt") Instant updatedAt);


    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.status = 'VERIFIED',
                e.verifiedAt = :verifiedAt,
                e.updatedAt = :updatedAt,
                e.verificationMethod = :method
            WHERE e.id = :ekycId
            """)
    void markAsVerified(@Param("ekycId") Integer ekycId,
                        @Param("verifiedAt") Instant verifiedAt,
                        @Param("updatedAt") Instant updatedAt,
                        @Param("method") String method);




    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EkycProfile e
            WHERE e.idCardNumberHash = :hash
              AND UPPER(e.status) = 'VERIFIED'
            """)
    boolean existsVerifiedByIdcardnumberhash(@Param("hash") String hash);

    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.idCardNumber     = :idCardNumber,
                e.idCardNumberHash = :idCardNumberHash,
                e.fullName         = :fullName,
                e.dateOfBirth      = :dateOfBirth,
                e.gender           = :gender,
                e.homeTown         = :hometown,
                e.provinceCode     = :provinceCode,
                e.provinceName     = :provinceName
            WHERE e.id = :ekycId
            """)
    void updateIdCardDetailsAndHash(@Param("ekycId")           Integer ekycId,
                                    @Param("idCardNumber")     String  idCardNumber,
                                    @Param("idCardNumberHash") String  idCardNumberHash,
                                    @Param("fullName")         String  fullName,
                                    @Param("dateOfBirth")      String  dateOfBirth,
                                    @Param("gender")           String  gender,
                                    @Param("hometown")         String  hometown,
                                    @Param("provinceCode")     String  provinceCode,
                                    @Param("provinceName")     String  provinceName);


    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EkycProfile e
            WHERE e.idCardNumberHash = :hash
              AND UPPER(e.status) = 'VERIFIED'
              AND e.user <> :user
            """)
    boolean existsVerifiedByIdcardnumberhashAndUseridNot(@Param("hash") String hash,
                                                         @Param("user") User user);
}
