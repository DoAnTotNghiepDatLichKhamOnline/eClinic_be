package iuh.fit.se.eclinic.catalog.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.catalog.dto.request.CapNhatAnhBacSiRequest;
import iuh.fit.se.eclinic.catalog.dto.response.AnhBacSiResponse;
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
 * CỐ Ý không có @Transactional ở class (như AnhDaiDienServiceImpl của identity-service): gọi kho ảnh có thể mất tới
 * 20 giây, không được giữ transaction / kết nối DB trong lúc đó. Phần ghi DB chạy trong transaction ngắn qua
 * {@link TransactionTemplate}.
 * <p>
 * Mỗi ảnh có mã riêng trong kho ({@code bac-si/<idBacSi>/<mã ngẫu nhiên>}) nên không ảnh nào ghi đè ảnh nào.
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
    public AnhBacSiResponse them(Long idBacSi, LoaiAnhBacSi loai, String chuThich, byte[] noiDung) {
        kiemTraCoBacSi(idBacSi);
        if (DinhDangAnh.nhanDien(noiDung).isEmpty()) {
            throw new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE);
        }
        if (!luuTruAnh.daCauHinh()) {
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
        // Kiểm tra trước khi tải lên để không tốn hạn mức kho ảnh; kiểm tra lại lúc ghi DB
        kiemTraConChoTrong(idBacSi);

        String ma = "bac-si/" + idBacSi + "/" + TokenNgauNhien.tao();
        String url = luuTruAnh.taiLen(ma, noiDung, CANH_TOI_DA);
        try {
            AnhBacSi anh = transactionTemplate.execute(trangThai -> {
                kiemTraConChoTrong(idBacSi);
                AnhBacSi moi = new AnhBacSi();
                moi.setBacSi(bacSiRepository.getReferenceById(idBacSi));
                moi.setLoai(loai);
                moi.setUrl(url);
                moi.setMaLuuTru(ma);
                moi.setChuThich(bacSiMapper.chuanHoa(chuThich));
                moi.setThuTu(anhBacSiRepository.timThuTuLonNhat(idBacSi) + 1);
                return anhBacSiRepository.save(moi);
            });
            log.info("Thêm ảnh giới thiệu id={} cho bác sĩ id={} ({} byte)", anh.getId(), idBacSi, noiDung.length);
            return bacSiMapper.toAnhResponse(anh);
        } catch (RuntimeException ex) {
            // Ảnh đã nằm trong kho nhưng không ghi được vào DB: dọn để không còn ảnh thừa trong kho
            xoaTrongKho(ma);
            throw ex;
        }
    }

    @Override
    public AnhBacSiResponse capNhat(Long idBacSi, Long idAnh, CapNhatAnhBacSiRequest request) {
        return transactionTemplate.execute(trangThai -> {
            AnhBacSi anh = timAnh(idBacSi, idAnh);
            anh.setLoai(request.loai());
            anh.setChuThich(bacSiMapper.chuanHoa(request.chuThich()));
            return bacSiMapper.toAnhResponse(anh);
        });
    }

    @Override
    public List<AnhBacSiResponse> sapXep(Long idBacSi, List<Long> idAnh) {
        kiemTraCoBacSi(idBacSi);
        return transactionTemplate.execute(trangThai -> {
            Map<Long, AnhBacSi> anhTheoId = anhBacSiRepository.findByBacSiIdOrderByThuTuAscIdAsc(idBacSi).stream()
                    .collect(Collectors.toMap(AnhBacSi::getId, Function.identity()));
            if (idAnh.size() != anhTheoId.size() || !new HashSet<>(idAnh).equals(anhTheoId.keySet())) {
                throw new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE,
                        "Danh sách phải gồm đúng id của mọi ảnh giới thiệu của bác sĩ, mỗi ảnh 1 lần");
            }
            int thuTu = 1;
            for (Long id : idAnh) {
                anhTheoId.get(id).setThuTu(thuTu++);
            }
            return idAnh.stream().map(anhTheoId::get).map(bacSiMapper::toAnhResponse).toList();
        });
    }

    @Override
    public void xoa(Long idBacSi, Long idAnh) {
        String ma = transactionTemplate.execute(trangThai -> {
            AnhBacSi anh = timAnh(idBacSi, idAnh);
            anhBacSiRepository.delete(anh);
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

    private void kiemTraConChoTrong(Long idBacSi) {
        if (anhBacSiRepository.countByBacSiId(idBacSi) >= SO_ANH_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.VUOT_SO_ANH_BAC_SI,
                    "Mỗi bác sĩ có tối đa " + SO_ANH_TOI_DA + " ảnh giới thiệu, hãy xoá bớt ảnh trước khi thêm");
        }
    }

    private AnhBacSi timAnh(Long idBacSi, Long idAnh) {
        return anhBacSiRepository.findByIdAndBacSiId(idAnh, idBacSi)
                .orElseThrow(() -> new LoiKhongTimThay("AnhBacSi", idAnh));
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
