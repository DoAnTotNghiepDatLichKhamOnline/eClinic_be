package iuh.fit.fe.be_websatlichkham.identity.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.fe.be_websatlichkham.common.entity.identity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndNgayThuHoiIsNull(String tokenHash);

    /** Danh sách phiên đăng nhập còn hiệu lực của tài khoản. */
    List<RefreshToken> findByTaiKhoanIdAndNgayThuHoiIsNullAndNgayHetHanAfterOrderByNgayTaoDesc(Long taiKhoanId,
            LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.ngayThuHoi = :now where t.taiKhoan.id = :taiKhoanId and t.ngayThuHoi is null")
    int revokeAllByTaiKhoanId(Long taiKhoanId, LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken t where t.ngayHetHan < :before")
    int deleteExpiredBefore(LocalDateTime before);

}
