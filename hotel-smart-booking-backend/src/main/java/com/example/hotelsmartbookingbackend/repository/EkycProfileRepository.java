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

    /**
     * Tìm bản ghi eKYC mới nhất của một user theo userid.
     */
    Optional<EkycProfile> findTopByUseridOrderByCreatedatDesc(User user);

    /**
     * Kiểm tra user đã có profile eKYC được xác minh thành công chưa.
     */
    boolean existsByUseridAndStatus(User user, String status);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EkycProfile e
            WHERE e.userid = :user
              AND UPPER(e.status) = 'VERIFIED'
            """)
    boolean existsVerifiedByUserid(@Param("user") User user);

    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.status = 'AI_CHECKING',
                e.updatedat = :updatedAt
            WHERE e.id = :ekycId
            """)
    void markAsAiChecking(@Param("ekycId") Integer ekycId,
                          @Param("updatedAt") Instant updatedAt);

    /**
     * Cập nhật trạng thái eKYC thành Verified.
     * Dùng @Modifying + @Query để thực hiện bulk update không cần load entity.
     */
    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.status = 'VERIFIED',
                e.verifiedat = :verifiedAt,
                e.updatedat = :updatedAt,
                e.verificationmethod = :method
            WHERE e.id = :ekycId
            """)
    void markAsVerified(@Param("ekycId") Integer ekycId,
                        @Param("verifiedAt") Instant verifiedAt,
                        @Param("updatedAt") Instant updatedAt,
                        @Param("method") String method);

    /**
     * Cập nhật số CCCD bóc tách tự động từ OCR vào bản ghi EkycProfile.
     */
    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.idcardnumber = :idCardNumber
            WHERE e.id = :ekycId
            """)
    void updateIdCardNumber(@Param("ekycId") Integer ekycId,
                            @Param("idCardNumber") String idCardNumber);

    /**
     * Kiểm tra số CCCD (qua HMAC-SHA256 fingerprint) đã được đăng ký bởi user khác chưa.
     *
     * <p>Không lấy plaintext từ DB – chỉ so sánh hash → an toàn và hiệu năng cao.
     *
     * @param hash Chuỗi HEX 64 ký tự từ {@code AesEncryptionService.hmac(idCardNumber)}
     * @return {@code true} nếu đã có bản ghi Verified với hash này
     */
    boolean existsByIdcardnumberhashAndStatus(String hash, String status);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EkycProfile e
            WHERE e.idcardnumberhash = :hash
              AND UPPER(e.status) = 'VERIFIED'
            """)
    boolean existsVerifiedByIdcardnumberhash(@Param("hash") String hash);

    /**
     * Cập nhật cả AES ciphertext lẫn HMAC hash cùng một lúc.
     * Hometown được suy từ 3 số đầu CCCD, không lấy từ OCR quê quán.
     */
    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.idcardnumber     = :idCardNumber,
                e.idcardnumberhash = :idCardNumberHash,
                e.fullname         = :fullName,
                e.dateofbirth      = :dateOfBirth,
                e.gender           = :gender,
                e.hometown         = :hometown,
                e.provincecode     = :provinceCode,
                e.provincename     = :provinceName
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

    /**
     * Kiểm tra số CCCD (qua HMAC-SHA256 fingerprint) đã được đăng ký bởi user khác chưa.
     */
    boolean existsByIdcardnumberhashAndStatusAndUseridNot(String hash, String status, User user);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EkycProfile e
            WHERE e.idcardnumberhash = :hash
              AND UPPER(e.status) = 'VERIFIED'
              AND e.userid <> :user
            """)
    boolean existsVerifiedByIdcardnumberhashAndUseridNot(@Param("hash") String hash,
                                                        @Param("user") User user);
}
