package iuh.fit.se.eclinic.catalog.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.se.eclinic.catalog.dto.response.BacSiChiTietResponse;
import iuh.fit.se.eclinic.catalog.dto.response.BacSiResponse;
import iuh.fit.se.eclinic.catalog.mapper.BacSiMapper;
import iuh.fit.se.eclinic.catalog.repository.AnhBacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.CauHinhChiDocRepository;
import iuh.fit.se.eclinic.catalog.repository.CauHinhChiDocRepository.QuyTacDatLich;
import iuh.fit.se.eclinic.catalog.repository.DanhGiaChiDocRepository;
import iuh.fit.se.eclinic.catalog.repository.DanhGiaChiDocRepository.DiemTheoBacSi;
import iuh.fit.se.eclinic.catalog.service.BacSiService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.enums.BuoiKham;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacSiServiceImpl implements BacSiService {

    private final BacSiRepository bacSiRepository;
    private final AnhBacSiRepository anhBacSiRepository;
    private final BacSiMapper bacSiMapper;
    private final DanhGiaChiDocRepository danhGiaChiDocRepository;
    private final CauHinhChiDocRepository cauHinhChiDocRepository;

    /** Khi booking-service chưa ghi dòng quy tắc đặt lịch: tìm chỗ trống trong chừng này ngày tới. */
    private static final int SO_NGAY_KHI_CHUA_CO_QUY_TAC = 365;
    /** Không lọc theo buổi: mọi giờ trong ngày. */
    private static final int GIO_DAU_NGAY = 0;
    private static final int GIO_CUOI_NGAY = 24;

    @Override
    public BacSi layTheoId(Long id) {
        return bacSiRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("BacSi", id));
    }

    @Override
    public Optional<BacSi> timTheoTaiKhoanId(Long taiKhoanId) {
        return bacSiRepository.findByTaiKhoanId(taiKhoanId);
    }

    @Override
    public List<BacSi> timTheoChuyenKhoa(Long chuyenKhoaId) {
        return bacSiRepository.findByChuyenKhoaId(chuyenKhoaId);
    }

    @Override
    public TrangDuLieu<BacSiResponse> timKiem(Long idChuyenKhoa, String tuKhoa, LocalDate ngay, BuoiKham buoi,
            int trang, int kichThuoc) {
        PageRequest phanTrang = PageRequest.of(trang, kichThuoc);
        // Thứ tự sắp xếp nằm trong câu query (theo họ tên ở bảng tai_khoan)
        Page<BacSi> ketQua = ngay == null && buoi == null
                ? bacSiRepository.timCongKhai(idChuyenKhoa, mauTen(tuKhoa), phanTrang)
                : timConCho(idChuyenKhoa, tuKhoa, ngay, buoi, phanTrang);
        List<Long> idCacBacSi = ketQua.getContent().stream().map(BacSi::getId).toList();
        Map<Long, DiemTheoBacSi> diem = idCacBacSi.isEmpty() ? Map.of()
                : danhGiaChiDocRepository.tinhDiem(idCacBacSi).stream()
                        .collect(Collectors.toMap(DiemTheoBacSi::getIdBacSi, Function.identity()));
        return TrangDuLieu.tu(ketQua.map(bacSi -> bacSiMapper.toResponse(bacSi, diem.get(bacSi.getId()))));
    }

    @Override
    public BacSiChiTietResponse layChiTiet(Long id) {
        BacSi bacSi = bacSiRepository.timCongKhaiTheoId(id).orElseThrow(() -> new LoiKhongTimThay("BacSi", id));
        return bacSiMapper.toChiTietResponse(bacSi, anhBacSiRepository.findByBacSiIdOrderByThuTuAscIdAsc(id),
                danhGiaChiDocRepository.tinhDiem(List.of(id)).stream().findFirst().orElse(null));
    }

    /**
     * DOC-03 "khung giờ rảnh": bác sĩ còn lượt khám đặt được, cùng điều kiện với khung giờ của booking-service (bắt đầu
     * sau bây giờ + thời gian đặt trước tối thiểu, trong số ngày đặt trước tối đa). Quy tắc đọc thẳng từ DB nên quản trị
     * viên sửa là có hiệu lực ngay.
     */
    private Page<BacSi> timConCho(Long idChuyenKhoa, String tuKhoa, LocalDate ngay, BuoiKham buoi,
            PageRequest phanTrang) {
        LocalDateTime bayGio = LocalDateTime.now();
        LocalDate homNay = bayGio.toLocalDate();
        QuyTacDatLich quyTac = cauHinhChiDocRepository.timQuyTacDatLich(CauHinhHeThong.ID).orElse(null);
        LocalDateTime moc = quyTac == null ? bayGio : bayGio.plusMinutes(quyTac.getDatTruocToiThieuPhut());
        LocalDate ngayXaNhat = homNay.plusDays(
                quyTac == null ? SO_NGAY_KHI_CHUA_CO_QUY_TAC : quyTac.getSoNgayDatTruocToiDa());
        LocalDate tuNgay = ngay != null ? ngay : homNay;
        LocalDate denNgay = ngay != null ? ngay : ngayXaNhat;
        // Như khung giờ: ngày ngoài khoảng đặt được thì không có gì, không báo lỗi
        if (tuNgay.isBefore(homNay) || denNgay.isAfter(ngayXaNhat)) {
            return Page.empty(phanTrang);
        }
        return bacSiRepository.timCongKhaiConCho(idChuyenKhoa, mauTen(tuKhoa), moc, tuNgay, denNgay,
                buoi == null ? GIO_DAU_NGAY : buoi.gioTu(), buoi == null ? GIO_CUOI_NGAY : buoi.gioDen(), phanTrang);
    }

    /** Mẫu LIKE "chứa từ khoá"; %, _ trong từ khoá được escape để chỉ khớp đúng ký tự đó. Rỗng -> null. */
    static String mauTen(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return null;
        }
        StringBuilder mau = new StringBuilder("%");
        for (char kyTu : tuKhoa.trim().toCharArray()) {
            if (kyTu == '%' || kyTu == '_' || kyTu == BacSiRepository.KY_TU_ESCAPE) {
                mau.append(BacSiRepository.KY_TU_ESCAPE);
            }
            mau.append(kyTu);
        }
        return mau.append('%').toString();
    }

}
