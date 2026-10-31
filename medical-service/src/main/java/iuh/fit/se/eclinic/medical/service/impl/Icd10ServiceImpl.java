package iuh.fit.se.eclinic.medical.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import iuh.fit.se.eclinic.common.entity.medical.Icd10;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.util.DanhMucIcd10;
import iuh.fit.se.eclinic.medical.service.Icd10Service;

/** Danh mục nằm trong tệp ({@link DanhMucIcd10}), không đọc DB. */
@Service
public class Icd10ServiceImpl implements Icd10Service {

    private static final int SO_GOI_Y_TOI_DA = 20;

    @Override
    public Icd10 layDeGhi(String ma) {
        String chuan = ma.trim().toUpperCase(Locale.ROOT);
        return DanhMucIcd10.tim(chuan)
                .orElseThrow(() -> new LoiNghiepVu(MaLoi.DU_LIEU_KHONG_HOP_LE, "loi.DU_LIEU_KHONG_HOP_LE.maIcd", chuan));
    }

    @Override
    public List<Icd10> timKiem(String tuKhoa) {
        if (!StringUtils.hasText(tuKhoa)) {
            return List.of();
        }
        String gon = tuKhoa.trim().replaceAll("\s+", " ");
        return DanhMucIcd10.goiY(gon, SO_GOI_Y_TOI_DA);
    }

}
