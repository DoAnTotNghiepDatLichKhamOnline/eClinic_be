package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import iuh.fit.se.eclinic.common.entity.medical.Thuoc;
import iuh.fit.se.eclinic.common.enums.TrangThaiThuoc;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.MauTimKiem;
import iuh.fit.se.eclinic.medical.repository.ThuocRepository;
import iuh.fit.se.eclinic.medical.service.ThuocService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThuocServiceImpl implements ThuocService {

    private static final int SO_GOI_Y_TOI_DA = 20;

    private final ThuocRepository thuocRepository;

    @Override
    public Map<Long, Thuoc> layDeKe(List<Long> idCacThuoc, Set<Long> idThuocDaCoTrongDon) {
        if (idCacThuoc.isEmpty()) {
            return Map.of();
        }
        // Không khoá: quản trị viên cho ngừng dùng đúng lúc bác sĩ đang lưu thì đơn đó vẫn giữ thuốc, như 1 đơn cũ
        Map<Long, Thuoc> theoId = thuocRepository.findAllById(idCacThuoc).stream()
                .collect(Collectors.toMap(Thuoc::getId, Function.identity()));
        for (Long id : idCacThuoc) {
            Thuoc thuoc = theoId.get(id);
            if (thuoc == null) {
                throw new LoiNghiepVu(MaLoi.KHONG_TIM_THAY, "loi.KHONG_TIM_THAY.thuoc", id);
            }
            if (thuoc.keDuoc() || idThuocDaCoTrongDon.contains(id)) {
                continue;
            }
            if (thuoc.getTrangThai() == TrangThaiThuoc.NGUNG_DUNG) {
                throw new LoiNghiepVu(MaLoi.THUOC_NGUNG_DUNG, "loi.THUOC_NGUNG_DUNG.ten", thuoc.getTenThuoc());
            }
            throw new LoiNghiepVu(MaLoi.THUOC_CHUA_XAC_MINH, "loi.THUOC_CHUA_XAC_MINH.ten", thuoc.getTenThuoc());
        }
        return theoId;
    }

    @Override
    public List<Thuoc> timKiem(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return thuocRepository.goiYKeDon(MauTimKiem.chua(normalize(keyword)), PageRequest.of(0, SO_GOI_Y_TOI_DA));
    }

    static String normalize(String ten) {
        return collapseWhitespace(ten).toLowerCase(Locale.ROOT);
    }

    static String collapseWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

}
