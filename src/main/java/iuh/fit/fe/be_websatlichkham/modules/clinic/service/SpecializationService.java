package iuh.fit.fe.be_websatlichkham.modules.clinic.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Specialization;

public interface SpecializationService {

    Specialization getById(Long id);

    List<Specialization> findAll();

}
