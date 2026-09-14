package iuh.fit.fe.be_websatlichkham.modules.clinic.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Specialization;
import iuh.fit.fe.be_websatlichkham.modules.clinic.repository.SpecializationRepository;
import iuh.fit.fe.be_websatlichkham.modules.clinic.service.SpecializationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecializationServiceImpl implements SpecializationService {

    private final SpecializationRepository specializationRepository;

    @Override
    public Specialization getById(Long id) {
        return specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization", id));
    }

    @Override
    public List<Specialization> findAll() {
        return specializationRepository.findAll(Sort.by("name"));
    }

}
