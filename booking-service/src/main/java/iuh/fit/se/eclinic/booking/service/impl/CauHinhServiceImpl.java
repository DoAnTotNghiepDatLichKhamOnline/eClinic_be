package iuh.fit.se.eclinic.booking.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.booking.config.DanhGiaProperties;
import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.config.LichLamViecProperties;
import iuh.fit.se.eclinic.booking.dto.request.CapNhatCauHinhRequest;
import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse;
import iuh.fit.se.eclinic.booking.dto.response.LichSuCauHinhResponse;
import iuh.fit.se.eclinic.booking.dto.response.QuyTacDatLichResponse;
import iuh.fit.se.eclinic.booking.mapper.CauHinhMapper;
import iuh.fit.se.eclinic.booking.repository.CauHinhHeThongRepository;
import iuh.fit.se.eclinic.booking.repository.LichSuCauHinhRepository;
import iuh.fit.se.eclinic.booking.repository.QuanTriVienChiDocRepository;
import iuh.fit.se.eclinic.booking.service.CauHinhHienTai;
import iuh.fit.se.eclinic.booking.service.CauHinhService;
import iuh.fit.se.eclinic.common.dto.TrangDuLieu;
import iuh.fit.se.eclinic.common.entity.booking.CauHinhHeThong;
import iuh.fit.se.eclinic.common.entity.booking.LichSuCauHinh;
import iuh.fit.se.eclinic.common.entity.identity.QuanTriVien;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.extern.slf4j.Slf4j;

/**
 * Không đặt {@code @Transactional} ở lớp: {@link #hienTai()} thường đọc từ bộ nhớ đệm, không cần transaction; khi phải
 * ghi dòng đầu tiên thì tự mở transaction riêng (bên gọi có thể đang trong transaction chỉ đọc).
 * <p>
 * Không phụ thuộc service nào khác của booking-service: các service đó đều gọi {@link #hienTai()}.
 */
@Slf4j
@Service
public class CauHinhServiceImpl implements CauHinhService {

    private static final Duration TUOI_TOI_DA = Duration.ofSeconds(60);

    /** Bản đọc được lúc {@code docLuc} ({@link System#nanoTime()}). */
    private record BanChup(CauHinhHienTai giaTri, long docLuc) {
    }

    /** Mỗi quy tắc: tên trường trong API (cũng là tên ghi vào lịch sử) và biến môi trường ghi giá trị đầu tiên. */
    private enum TruongCauHinh {
        DAT_TRUOC_TOI_THIEU_PHUT("datTruocToiThieuPhut", "BOOKING_MIN_LEAD", CauHinhHienTai::datTruocToiThieuPhut),
        SO_NGAY_DAT_TRUOC_TOI_DA("soNgayDatTruocToiDa", "BOOKING_MAX_DAYS", CauHinhHienTai::soNgayDatTruocToiDa),
        SO_LICH_MOI_HO_SO("soLichHieuLucToiDaMoiHoSo", "BOOKING_MAX_ACTIVE_PER_PATIENT",
                CauHinhHienTai::soLichHieuLucToiDaMoiHoSo),
        SO_LICH_MOI_SO_DIEN_THOAI("soLichHieuLucToiDaMoiSoDienThoai", "BOOKING_MAX_ACTIVE_PER_PHONE",
                CauHinhHienTai::soLichHieuLucToiDaMoiSoDienThoai),
        HUY_DOI_TRUOC_TOI_THIEU_PHUT("huyDoiTruocToiThieuPhut", "BOOKING_CHANGE_LEAD",
                CauHinhHienTai::huyDoiTruocToiThieuPhut),
        SO_LAN_DOI_LICH_TOI_DA("soLanDoiLichToiDa", "BOOKING_MAX_RESCHEDULES", CauHinhHienTai::soLanDoiLichToiDa),
        GUI_YEU_CAU_TRUOC_TOI_THIEU_GIO("guiYeuCauTruocToiThieuGio", "SHIFT_REQUEST_LEAD",
                CauHinhHienTai::guiYeuCauTruocToiThieuGio),
        SO_NGAY_XEP_TRUOC_TOI_DA("soNgayXepTruocToiDa", "SHIFT_MAX_DAYS", CauHinhHienTai::soNgayXepTruocToiDa),
        SO_GIO_TOI_DA_MOI_TUAN("soGioToiDaMoiTuan", "SHIFT_MAX_WEEKLY_HOURS", CauHinhHienTai::soGioToiDaMoiTuan),
        SO_NGAY_DUOC_SUA_DANH_GIA("soNgayDuocSuaDanhGia", "RATING_EDIT_DAYS", CauHinhHienTai::soNgayDuocSuaDanhGia);

        private final String ten;
        private final String bienMoiTruong;
        private final ToIntFunction<CauHinhHienTai> doc;

        TruongCauHinh(String ten, String bienMoiTruong, ToIntFunction<CauHinhHienTai> doc) {
            this.ten = ten;
            this.bienMoiTruong = bienMoiTruong;
            this.doc = doc;
        }
    }

    private final CauHinhHeThongRepository cauHinhHeThongRepository;
    private final LichSuCauHinhRepository lichSuCauHinhRepository;
    private final QuanTriVienChiDocRepository quanTriVienChiDocRepository;
    private final CauHinhMapper cauHinhMapper;
    private final CauHinhHienTai bienMoiTruong;
    private final TransactionTemplate giaoDichRieng;

    private volatile BanChup banChup;
    /** Tăng mỗi lần xoá bộ nhớ đệm: lần đọc bắt đầu trước khi xoá không được ghi kết quả (đã cũ) vào bộ nhớ đệm. */
    private final AtomicLong theHe = new AtomicLong();

    public CauHinhServiceImpl(CauHinhHeThongRepository cauHinhHeThongRepository,
            LichSuCauHinhRepository lichSuCauHinhRepository, QuanTriVienChiDocRepository quanTriVienChiDocRepository,
            CauHinhMapper cauHinhMapper, DatLichProperties datLichProperties,
            LichLamViecProperties lichLamViecProperties, DanhGiaProperties danhGiaProperties,
            PlatformTransactionManager transactionManager) {
        this.cauHinhHeThongRepository = cauHinhHeThongRepository;
        this.lichSuCauHinhRepository = lichSuCauHinhRepository;
        this.quanTriVienChiDocRepository = quanTriVienChiDocRepository;
        this.cauHinhMapper = cauHinhMapper;
        this.bienMoiTruong = CauHinhHienTai.tuBienMoiTruong(datLichProperties, lichLamViecProperties,
                danhGiaProperties);
        this.giaoDichRieng = new TransactionTemplate(transactionManager);
        this.giaoDichRieng.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public CauHinhHienTai hienTai() {
        BanChup daDoc = banChup;
        if (daDoc != null && System.nanoTime() - daDoc.docLuc() < TUOI_TOI_DA.toNanos()) {
            return daDoc.giaTri();
        }
        long theHeLucDoc = theHe.get();
        CauHinhHienTai giaTri = cauHinhHeThongRepository.findById(CauHinhHeThong.ID)
                .map(CauHinhHienTai::tu)
                .orElseGet(() -> CauHinhHienTai.tu(giaoDichRieng.execute(trangThai -> taoNeuChuaCo())));
        if (theHe.get() == theHeLucDoc) {
            banChup = new BanChup(giaTri, System.nanoTime());
        }
        return giaTri;
    }

    @Override
    @Transactional
    public CauHinhQuanTriResponse layChoQuanTri() {
        return cauHinhMapper.toQuanTriResponse(cauHinhHeThongRepository.timKemQuanTriSua(CauHinhHeThong.ID)
                .orElseGet(this::taoNeuChuaCo));
    }

    @Override
    @Transactional
    public CauHinhQuanTriResponse capNhat(Long idTaiKhoanQuanTri, CapNhatCauHinhRequest request) {
        CauHinhHienTai moi = cauHinhMapper.toCauHinh(request);
        if (moi.soLichHieuLucToiDaMoiSoDienThoai() < moi.soLichHieuLucToiDaMoiHoSo()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.cauHinhSoLichMoiSoDienThoai");
        }
        QuanTriVien quanTri = quanTriVienChiDocRepository.findByTaiKhoanId(idTaiKhoanQuanTri)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN));

        // Khoá dòng rồi so phiên bản: 2 quản trị viên lưu cùng lúc thì chạy lần lượt, người sau bị từ chối
        CauHinhHeThong dong = cauHinhHeThongRepository.findByIdForUpdate(CauHinhHeThong.ID)
                .orElseGet(this::taoNeuChuaCo);
        if (request.phienBan() != dong.getPhienBan()) {
            throw new LoiNghiepVu(MaLoi.DU_LIEU_DA_THAY_DOI);
        }

        CauHinhHienTai cu = CauHinhHienTai.tu(dong);
        int soTruongDoi = 0;
        for (TruongCauHinh truong : TruongCauHinh.values()) {
            int giaTriCu = truong.doc.applyAsInt(cu);
            int giaTriMoi = truong.doc.applyAsInt(moi);
            if (giaTriCu != giaTriMoi) {
                LichSuCauHinh lichSu = new LichSuCauHinh();
                lichSu.setTenTruong(truong.ten);
                lichSu.setGiaTriCu(String.valueOf(giaTriCu));
                lichSu.setGiaTriMoi(String.valueOf(giaTriMoi));
                lichSu.setQuanTri(quanTri);
                lichSuCauHinhRepository.save(lichSu);
                soTruongDoi++;
            }
        }
        if (soTruongDoi == 0) {
            return cauHinhMapper.toQuanTriResponse(dong);
        }

        cauHinhMapper.capNhat(dong, moi);
        dong.setPhienBan(dong.getPhienBan() + 1);
        dong.setNgaySua(LocalDateTime.now());
        dong.setQuanTriSua(quanTri);
        xoaBanChupSauCommit();
        log.info("Quản trị viên id={} sửa {} quy tắc đặt lịch / xếp ca (phiên bản {})", quanTri.getId(), soTruongDoi,
                dong.getPhienBan());
        return cauHinhMapper.toQuanTriResponse(dong);
    }

    @Override
    @Transactional(readOnly = true)
    public TrangDuLieu<LichSuCauHinhResponse> lichSu(int trang, int kichThuoc) {
        return TrangDuLieu.tu(lichSuCauHinhRepository.timMoiNhatTruoc(PageRequest.of(trang, kichThuoc))
                .map(cauHinhMapper::toLichSuResponse));
    }

    @Override
    public QuyTacDatLichResponse quyTacCongKhai() {
        return cauHinhMapper.toQuyTacDatLich(hienTai());
    }

    @Override
    @Transactional
    public void khoiTao() {
        Optional<CauHinhHeThong> dong = cauHinhHeThongRepository.findById(CauHinhHeThong.ID);
        if (dong.isEmpty()) {
            taoNeuChuaCo();
            return;
        }
        CauHinhHienTai dangLuu = CauHinhHienTai.tu(dong.get());
        for (TruongCauHinh truong : TruongCauHinh.values()) {
            int giaTriLuu = truong.doc.applyAsInt(dangLuu);
            int giaTriBien = truong.doc.applyAsInt(bienMoiTruong);
            if (giaTriLuu != giaTriBien) {
                log.warn("Cấu hình {}: biến môi trường {} cho {} nhưng DB đang lưu {}. Dùng giá trị trong DB; biến môi"
                        + " trường chỉ ghi lần đầu, sửa ở /api/booking/quan-tri/cau-hinh", truong.ten,
                        truong.bienMoiTruong, giaTriBien, giaTriLuu);
            }
        }
    }

    /**
     * Thêm dòng cấu hình từ biến môi trường nếu chưa có, rồi đọc khoá dòng. Đọc có khoá luôn thấy bản đã commit mới nhất,
     * kể cả khi 1 request khác vừa thêm dòng trước. Phải chạy trong transaction được ghi.
     */
    private CauHinhHeThong taoNeuChuaCo() {
        cauHinhHeThongRepository.themNeuChuaCo(bienMoiTruong.datTruocToiThieuPhut(),
                bienMoiTruong.soNgayDatTruocToiDa(), bienMoiTruong.soLichHieuLucToiDaMoiHoSo(),
                bienMoiTruong.soLichHieuLucToiDaMoiSoDienThoai(), bienMoiTruong.huyDoiTruocToiThieuPhut(),
                bienMoiTruong.soLanDoiLichToiDa(), bienMoiTruong.guiYeuCauTruocToiThieuGio(),
                bienMoiTruong.soNgayXepTruocToiDa(), bienMoiTruong.soGioToiDaMoiTuan(),
                bienMoiTruong.soNgayDuocSuaDanhGia());
        log.info("Chưa có dòng cấu hình hệ thống: đã ghi từ biến môi trường");
        return cauHinhHeThongRepository.findByIdForUpdate(CauHinhHeThong.ID)
                .orElseThrow(() -> new IllegalStateException("Không thêm được dòng cau_hinh_he_thong"));
    }

    private void xoaBanChupSauCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            xoaBanChup();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                xoaBanChup();
            }
        });
    }

    private void xoaBanChup() {
        theHe.incrementAndGet();
        banChup = null;
    }

}
