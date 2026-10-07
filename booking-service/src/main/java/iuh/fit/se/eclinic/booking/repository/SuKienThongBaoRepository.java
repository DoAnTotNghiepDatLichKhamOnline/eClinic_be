package iuh.fit.se.eclinic.booking.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.common.entity.booking.SuKienThongBao;

public interface SuKienThongBaoRepository extends JpaRepository<SuKienThongBao, Long> {

    /** Tối đa 100 sự kiện chưa gửi, cũ nhất trước; bỏ qua dòng đã bị notification-service từ chối đủ số lần. */
    List<SuKienThongBao> findTop100ByNgayGuiIsNullAndSoLanLoiLessThanOrderByIdAsc(int soLanLoiToiDa);

    @Transactional
    @Modifying
    @Query("update SuKienThongBao s set s.ngayGui = :bayGio where s.id in :cacId and s.ngayGui is null")
    int danhDauDaGui(Collection<Long> cacId, LocalDateTime bayGio);

    @Transactional
    @Modifying
    @Query("update SuKienThongBao s set s.soLanLoi = s.soLanLoi + 1 where s.id = :id")
    int tangSoLanLoi(Long id);

    @Transactional
    @Modifying
    @Query("delete from SuKienThongBao s where s.ngayGui < :truoc")
    int xoaDaGuiTruoc(LocalDateTime truoc);

}
