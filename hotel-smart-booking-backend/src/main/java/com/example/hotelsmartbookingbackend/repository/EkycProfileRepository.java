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

    /**
     * Cập nhật trạng thái eKYC thành Verified.
     * Dùng @Modifying + @Query để thực hiện bulk update không cần load entity.
     */
    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.status = 'Verified',
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
     * Cập nhật trạng thái eKYC thành Rejected kèm lý do.
     */
    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.status = 'Rejected',
                e.rejectionreason = :reason,
                e.updatedat = :updatedAt
            WHERE e.id = :ekycId
            """)
    void markAsRejected(@Param("ekycId") Integer ekycId,
                        @Param("reason") String reason,
                        @Param("updatedAt") Instant updatedAt);

    /**
     * Cập nhật số CCCD bóc tách tự động từ OCR vào bản ghi EkycProfile.
     * Được gọi ngay sau markAsVerified() để ghi số CCCD đọc được từ ảnh.
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

    /**
     * Cập nhật cả AES ciphertext lẫn HMAC hash cùng một lúc.
     */
    @Modifying
    @Query("""
            UPDATE EkycProfile e
            SET e.idcardnumber     = :idCardNumber,
                e.idcardnumberhash = :idCardNumberHash,
                e.fullname         = :fullName,
                e.dateofbirth      = :dateOfBirth
            WHERE e.id = :ekycId
            """)
    void updateIdCardDetailsAndHash(@Param("ekycId")           Integer ekycId,
                                    @Param("idCardNumber")     String  idCardNumber,
                                    @Param("idCardNumberHash") String  idCardNumberHash,
                                    @Param("fullName")         String  fullName,
                                    @Param("dateOfBirth")      String  dateOfBirth);

    /**
     * Kiểm tra số CCCD (qua HMAC-SHA256 fingerprint) đã được đăng ký bởi user khác chưa.
     */
    boolean existsByIdcardnumberhashAndStatusAndUseridNot(String hash, String status, User user);
}
