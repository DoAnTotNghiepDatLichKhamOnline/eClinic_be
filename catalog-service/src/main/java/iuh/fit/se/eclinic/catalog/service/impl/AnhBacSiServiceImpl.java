package iuh.fit.se.eclinic.catalog.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiQuanTriResponse;
import iuh.fit.se.eclinic.catalog.mapper.BacSiMapper;
import iuh.fit.se.eclinic.catalog.repository.AnhBacSiRepository;
import iuh.fit.se.eclinic.catalog.repository.BacSiRepository;
import iuh.fit.se.eclinic.catalog.service.AnhBacSiService;
import iuh.fit.se.eclinic.common.entity.catalog.AnhBacSi;
import iuh.fit.se.eclinic.common.enums.LoaiAnhBacSi;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.luutru.LuuTruAnh;
import iuh.fit.se.eclinic.common.util.DinhDangAnh;
import iuh.fit.se.eclinic.common.util.TokenNgauNhien;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Không có @Transactional ở class: gọi kho ảnh có thể mất 20 giây, không được giữ kết nối DB. Ảnh là 1 danh sách JSON trên
 * dòng bac_si ({@link AnhBacSiRepository}): mỗi lần sửa khoá dòng, đọc, sửa danh sách rồi ghi lại trong 1 transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnhBacSiServiceImpl implements AnhBacSiService {

    /** Cạnh dài nhất (pixel) của ảnh giới thiệu sau khi lưu. */
    private static final int CANH_TOI_DA = 1600;

    private final BacSiRepository bacSiRepository;
    private final AnhBacSiRepository anhBacSiRepository;
    private final BacSiMapper bacSiMapper;
    private final LuuTruAnh luuTruAnh;
    private final TransactionTemplate transactionTemplate;

    @Override
    public AnhBacSiQuanTriResponse them(Long idBacSi, LoaiAnhBacSi loai, String chuThich, String chuThichEn,
            byte[] noiDung) {
        kiemTraCoBacSi(idBacSi);
        if (DinhDangAnh.nhanDien(noiDung).isEmpty()) {
            throw new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE);
        }
        if (!luuTruAnh.daCauHinh()) {
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
        // Kiểm tra trước khi tải lên để không tốn hạn mức kho ảnh; kiểm tra lại lúc ghi DB
        kiemTraConChoTrong(anhBacSiRepository.countByBacSiId(idBacSi));

        String ma = "bac-si/" + idBacSi + "/" + TokenNgauNhien.tao();
        String url = luuTruAnh.taiLen(ma, noiDung, CANH_TOI_DA);
        try {
            AnhBacSi anh = transactionTemplate.execute(trangThai -> {
                List<AnhBacSi> danhSach = anhBacSiRepository.khoa(idBacSi);
                kiemTraConChoTrong(danhSach.size());
                AnhBacSi moi = new AnhBacSi();
                moi.setId(anhBacSiRepository.idMoi(idBacSi));
                moi.setLoai(loai);
                moi.setUrl(url);
                moi.setMaLuuTru(ma);
                moi.setChuThich(bacSiMapper.chuanHoa(chuThich));
                moi.setChuThichEn(bacSiMapper.chuanHoa(chuThichEn));
                moi.setThuTu(danhSach.stream().mapToInt(AnhBacSi::getThuTu).max().orElse(0) + 1);
                moi.setNgayTao(LocalDateTime.now());
                danhSach.add(moi);
                anhBacSiRepository.ghi(idBacSi, danhSach);
                return moi;
            });
            log.info("Thêm ảnh giới thiệu id={} cho bác sĩ id={} ({} byte)", anh.getId(), idBacSi, noiDung.length);
            return bacSiMapper.toAnhQuanTriResponse(anh);
        } catch (RuntimeException ex) {
            // Ảnh đã nằm trong kho nhưng không ghi được vào DB: dọn để không còn ảnh thừa trong kho
            xoaTrongKho(ma);
            throw ex;
        }
    }

    @Override
    public AnhBacSiQuanTriResponse capNhat(Long idBacSi, Long idAnh, CapNhatAnhBacSiRequest request) {
        return transactionTemplate.execute(trangThai -> {
            List<AnhBacSi> danhSach = anhBacSiRepository.khoa(idBacSi);
            AnhBacSi anh = timAnh(danhSach, idAnh);
            anh.setLoai(request.loai());
            anh.setChuThich(bacSiMapper.chuanHoa(request.chuThich()));
            anh.setChuThichEn(bacSiMapper.chuanHoa(request.chuThichEn()));
            anhBacSiRepository.ghi(idBacSi, danhSach);
            return bacSiMapper.toAnhQuanTriResponse(anh);
        });
    }

    @Override
    public List<AnhBacSiQuanTriResponse> sapXep(Long idBacSi, List<Long> idAnh) {
        kiemTraCoBacSi(idBacSi);
        return transactionTemplate.execute(trangThai -> {
            List<AnhBacSi> danhSach = anhBacSiRepository.khoa(idBacSi);
            Map<Long, AnhBacSi> anhTheoId = danhSach.stream()
                    .collect(Collectors.toMap(AnhBacSi::getId, Function.identity()));
            if (idAnh.size() != anhTheoId.size() || !new HashSet<>(idAnh).equals(anhTheoId.keySet())) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                        "loi.DU_LIEU_KHONG_HOP_LE.sapXepAnh");
            }
            int thuTu = 1;
            for (Long id : idAnh) {
                anhTheoId.get(id).setThuTu(thuTu++);
            }
            anhBacSiRepository.ghi(idBacSi, danhSach);
            return idAnh.stream().map(anhTheoId::get).map(bacSiMapper::toAnhQuanTriResponse).toList();
        });
    }

    @Override
    public AnhBacSiQuanTriResponse capNhatChuThichAnhCongViec(Long idBacSi, Long idAnh, String chuThich,
            String chuThichEn) {
        return transactionTemplate.execute(trangThai -> {
            List<AnhBacSi> danhSach = anhBacSiRepository.khoa(idBacSi);
            AnhBacSi anh = timAnhCongViec(danhSach, idAnh);
            anh.setChuThich(bacSiMapper.chuanHoa(chuThich));
            anh.setChuThichEn(bacSiMapper.chuanHoa(chuThichEn));
            anhBacSiRepository.ghi(idBacSi, danhSach);
            return bacSiMapper.toAnhQuanTriResponse(anh);
        });
    }

    @Override
    public void xoa(Long idBacSi, Long idAnh) {
        xoa(idBacSi, idAnh, false);
    }

    @Override
    public void xoaAnhCongViec(Long idBacSi, Long idAnh) {
        xoa(idBacSi, idAnh, true);
    }

    private void xoa(Long idBacSi, Long idAnh, boolean chiAnhCongViec) {
        String ma = transactionTemplate.execute(trangThai -> {
            List<AnhBacSi> danhSach = anhBacSiRepository.khoa(idBacSi);
            AnhBacSi anh = chiAnhCongViec ? timAnhCongViec(danhSach, idAnh) : timAnh(danhSach, idAnh);
            danhSach.remove(anh);
            anhBacSiRepository.ghi(idBacSi, danhSach);
            return anh.getMaLuuTru();
        });
        log.info("Xoá ảnh giới thiệu id={} của bác sĩ id={}", idAnh, idBacSi);
        // null: ảnh nằm ngoài kho (dữ liệu mẫu)
        if (ma != null) {
            xoaTrongKho(ma);
        }
    }

    private void kiemTraCoBacSi(Long idBacSi) {
        if (!bacSiRepository.existsById(idBacSi)) {
            throw new LoiKhongTimThay("BacSi", idBacSi);
        }
    }

    private void kiemTraConChoTrong(long soAnh) {
        if (soAnh >= SO_ANH_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.VUOT_SO_ANH_BAC_SI,
                    "loi.VUOT_SO_ANH_BAC_SI.toiDa", String.valueOf(SO_ANH_TOI_DA));
        }
    }

    /** Ảnh trong danh sách của đúng bác sĩ này; ảnh của bác sĩ khác coi như không có. */
    private AnhBacSi timAnh(List<AnhBacSi> danhSach, Long idAnh) {
        return danhSach.stream().filter(anh -> anh.getId().equals(idAnh)).findFirst()
                .orElseThrow(() -> new LoiKhongTimThay("AnhBacSi", idAnh));
    }

    /** Ảnh chứng chỉ: bác sĩ xem được nhưng chỉ quản trị viên sửa / xoá. */
    private AnhBacSi timAnhCongViec(List<AnhBacSi> danhSach, Long idAnh) {
        AnhBacSi anh = timAnh(danhSach, idAnh);
        if (anh.getLoai() != LoaiAnhBacSi.ANH_CONG_VIEC) {
            throw new LoiNghiepVu(MaLoi.KHONG_CO_QUYEN, "loi.KHONG_CO_QUYEN.anhChungChi");
        }
        return anh;
    }

    /** Lỗi kho ảnh không ném ra: dòng trong DB đã xoá / chưa từng có, chỉ còn ảnh thừa trong kho phải xoá tay. */
    private void xoaTrongKho(String ma) {
        try {
            luuTruAnh.xoa(ma);
        } catch (LoiNghiepVu ex) {
            log.warn("Không xoá được ảnh giới thiệu trong kho ảnh (ảnh còn sót lại: {})", ma);
        }
    }

}
