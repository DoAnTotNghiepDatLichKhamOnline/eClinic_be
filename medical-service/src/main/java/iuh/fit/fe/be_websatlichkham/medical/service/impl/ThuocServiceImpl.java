package iuh.fit.fe.be_websatlichkham.medical.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import iuh.fit.fe.be_websatlichkham.common.entity.medical.Thuoc;
import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.medical.repository.ThuocRepository;
import iuh.fit.fe.be_websatlichkham.medical.service.ThuocService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThuocServiceImpl implements ThuocService {

    private final ThuocRepository thuocRepository;

    @Override
    @Transactional
    public Thuoc getOrCreate(String tenThuoc, String donVi, Long idBacSiTao) {
        if (!StringUtils.hasText(tenThuoc)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Tên thuốc không được để trống");
        }
        String tenHienThi = collapseWhitespace(tenThuoc);
        String tenChuanHoa = normalize(tenThuoc);

        return thuocRepository.findByTenChuanHoa(tenChuanHoa).orElseGet(() -> {
            // INSERT ... ON DUPLICATE KEY UPDATE: không ném lỗi nếu request khác vừa thêm cùng tên,
            // sau đó đọc có khoá để chắc chắn thấy bản ghi đã commit.
            thuocRepository.insertIfAbsent(tenHienThi, tenChuanHoa, donVi, idBacSiTao);
            return thuocRepository.findByTenChuanHoaForShare(tenChuanHoa)
                    .orElseThrow(() -> new IllegalStateException("Thuoc not found after insert: " + tenChuanHoa));
        });
    }

    @Override
    public List<Thuoc> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return thuocRepository.findTop20ByTenChuanHoaContainingOrderByTenThuocAsc(normalize(keyword));
    }

    static String normalize(String ten) {
        return collapseWhitespace(ten).toLowerCase(Locale.ROOT);
    }

    private static String collapseWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

}
