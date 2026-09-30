package iuh.fit.se.eclinic.identity.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.eclinic.common.entity.identity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Tìm theo hash, kể cả phiên đã thu hồi / hết hạn (để phát hiện dùng lại token cũ). */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndNgayThuHoiIsNull(String tokenHash);

    /** Danh sách phiên đăng nhập còn hiệu lực của tài khoản. */
    List<RefreshToken> findByTaiKhoanIdAndNgayThuHoiIsNullAndNgayHetHanAfterOrderByNgayTaoDesc(Long taiKhoanId,
            LocalDateTime now);

    /**
     * Thu hồi nếu chưa bị thu hồi. UPDATE có điều kiện là thao tác nguyên tử: 2 request cùng dùng 1 token thì chỉ
     * 1 request nhận được 1 (row lock của InnoDB), request kia nhận 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.ngayThuHoi = :now where t.id = :id and t.ngayThuHoi is null")
    int revokeIfActive(Long id, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.ngayThuHoi = :now where t.taiKhoan.id = :taiKhoanId and t.ngayThuHoi is null")
    int revokeAllByTaiKhoanId(Long taiKhoanId, LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken t where t.ngayHetHan < :before")
    int deleteExpiredBefore(LocalDateTime before);

}
