package iuh.fit.fe.be_websatlichkham.modules.medical.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.modules.medical.entity.Medication;
import iuh.fit.fe.be_websatlichkham.modules.medical.repository.MedicationRepository;
import iuh.fit.fe.be_websatlichkham.modules.medical.service.MedicationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationServiceImpl implements MedicationService {

    private final MedicationRepository medicationRepository;

    @Override
    @Transactional
    public Medication getOrCreate(String name, String unit, Long createdByDoctorId) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Tên thuốc không được để trống");
        }
        String displayName = collapseWhitespace(name);
        String normalizedName = normalize(name);

        return medicationRepository.findByNormalizedName(normalizedName).orElseGet(() -> {
            // INSERT ... ON DUPLICATE KEY UPDATE: không ném lỗi nếu request khác vừa thêm cùng tên,
            // sau đó đọc có khoá để chắc chắn thấy bản ghi đã commit.
            medicationRepository.insertIfAbsent(displayName, normalizedName, unit, createdByDoctorId);
            return medicationRepository.findByNormalizedNameForShare(normalizedName)
                    .orElseThrow(() -> new IllegalStateException("Medication not found after insert: " + normalizedName));
        });
    }

    @Override
    public List<Medication> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return medicationRepository.findTop20ByNormalizedNameContainingOrderByNameAsc(normalize(keyword));
    }

    static String normalize(String name) {
        return collapseWhitespace(name).toLowerCase(Locale.ROOT);
    }

    private static String collapseWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

}
