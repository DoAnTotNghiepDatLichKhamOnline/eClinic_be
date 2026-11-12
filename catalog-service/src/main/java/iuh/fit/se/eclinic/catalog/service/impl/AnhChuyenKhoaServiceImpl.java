package iuh.fit.se.eclinic.catalog.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import iuh.fit.se.eclinic.catalog.config.CacheConfig;
import iuh.fit.se.eclinic.catalog.dto.response.ChuyenKhoaQuanTriResponse;
import iuh.fit.se.eclinic.catalog.mapper.ChuyenKhoaMapper;
import iuh.fit.se.eclinic.catalog.repository.ChuyenKhoaRepository;
import iuh.fit.se.eclinic.catalog.service.AnhChuyenKhoaService;
import iuh.fit.se.eclinic.catalog.service.ChuyenKhoaDaXoaEvent;
import iuh.fit.se.eclinic.common.entity.catalog.ChuyenKhoa;
import iuh.fit.se.eclinic.common.exception.LoiKhongTimThay;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.luutru.LuuTruAnh;
import iuh.fit.se.eclinic.common.util.DinhDangAnh;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Không @Transactional ở class: gọi kho ảnh có thể mất tới 20 giây, không giữ kết nối DB trong lúc đó. Chỉ xoá theo mã
 * riêng của chuyên khoa, không theo URL: ảnh mẫu dùng chung nằm cùng thư mục kho nên xoá theo URL là xoá nhầm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnhChuyenKhoaServiceImpl implements AnhChuyenKhoaService {

    /** Ảnh mẫu 640×400; chừa chỗ cho ảnh bìa rộng ở trang chuyên khoa. */
    private static final int CANH_TOI_DA = 1280;

    private final ChuyenKhoaRepository chuyenKhoaRepository;
    private final ChuyenKhoaMapper chuyenKhoaMapper;
    private final LuuTruAnh luuTruAnh;
    private final TransactionTemplate transactionTemplate;

    @Override
    @CacheEvict(cacheNames = {CacheConfig.CHUYEN_KHOA, CacheConfig.CHUYEN_KHOA_CHI_TIET}, allEntries = true)
    public ChuyenKhoaQuanTriResponse taiLen(Long idChuyenKhoa, byte[] noiDung) {
        if (!chuyenKhoaRepository.existsById(idChuyenKhoa)) {
            throw new LoiKhongTimThay("ChuyenKhoa", idChuyenKhoa);
        }
        if (DinhDangAnh.nhanDien(noiDung).isEmpty()) {
            throw new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE);
        }
        if (!luuTruAnh.daCauHinh()) {
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }

        // Cùng mã với ảnh cũ nên kho ghi đè; URL mới mang số phiên bản mới nên trình duyệt / CDN không dùng ảnh cũ
        String ma = maAnh(idChuyenKhoa);
        String url = luuTruAnh.taiLen(ma, noiDung, CANH_TOI_DA);
        try {
            ChuyenKhoaQuanTriResponse ketQua = transactionTemplate.execute(trangThai -> {
                ChuyenKhoa chuyenKhoa = timEntity(idChuyenKhoa);
                chuyenKhoa.setAnhMinhHoa(url);
                return chuyenKhoaMapper.toQuanTriResponse(chuyenKhoa);
            });
            log.info("Đổi ảnh minh hoạ chuyên khoa id={} ({} byte)", idChuyenKhoa, noiDung.length);
            return ketQua;
        } catch (RuntimeException ex) {
            // Chuyên khoa bị xoá giữa chừng: ảnh đã vào kho nhưng không còn dòng nào trỏ tới, dọn đi
            xoaTrongKho(idChuyenKhoa);
            throw ex;
        }
    }

    @Override
    @CacheEvict(cacheNames = {CacheConfig.CHUYEN_KHOA, CacheConfig.CHUYEN_KHOA_CHI_TIET}, allEntries = true)
    public ChuyenKhoaQuanTriResponse xoa(Long idChuyenKhoa) {
        String[] anhCu = new String[1];
        ChuyenKhoaQuanTriResponse ketQua = transactionTemplate.execute(trangThai -> {
            ChuyenKhoa chuyenKhoa = timEntity(idChuyenKhoa);
            anhCu[0] = chuyenKhoa.getAnhMinhHoa();
            chuyenKhoa.setAnhMinhHoa(null);
            return chuyenKhoaMapper.toQuanTriResponse(chuyenKhoa);
        });
        if (anhCu[0] != null) {
            if (luuTruAnh.laAnhCuaKho(anhCu[0])) {
                xoaTrongKho(idChuyenKhoa);
            }
            log.info("Bỏ ảnh minh hoạ chuyên khoa id={}", idChuyenKhoa);
        }
        return ketQua;
    }

    /** Đồng bộ: catalog không bật @Async, và xoá chuyên khoa hiếm khi xảy ra. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void donAnhCuaChuyenKhoaDaXoa(ChuyenKhoaDaXoaEvent event) {
        if (event.anhMinhHoa() != null && luuTruAnh.laAnhCuaKho(event.anhMinhHoa())) {
            xoaTrongKho(event.idChuyenKhoa());
        }
    }

    /**
     * Ảnh mẫu không nằm ở mã này nên xoá ở đây không bao giờ chạm tới nó; mã chưa có ảnh thì kho trả "not found", không
     * phải lỗi. Lỗi kho ảnh không ném ra: dòng trong DB đã đúng, chỉ còn ảnh thừa phải xoá tay.
     */
    private void xoaTrongKho(Long idChuyenKhoa) {
        try {
            luuTruAnh.xoa(maAnh(idChuyenKhoa));
        } catch (LoiNghiepVu ex) {
            log.warn("Không xoá được ảnh minh hoạ của chuyên khoa id={} trong kho ảnh (ảnh còn sót lại: {})",
                    idChuyenKhoa, maAnh(idChuyenKhoa));
        }
    }

    private ChuyenKhoa timEntity(Long id) {
        return chuyenKhoaRepository.findById(id).orElseThrow(() -> new LoiKhongTimThay("ChuyenKhoa", id));
    }

    private static String maAnh(Long idChuyenKhoa) {
        return "chuyen-khoa/" + idChuyenKhoa;
    }

}
