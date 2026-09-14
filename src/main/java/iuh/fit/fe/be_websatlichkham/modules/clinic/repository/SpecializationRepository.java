package iuh.fit.fe.be_websatlichkham.modules.clinic.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Specialization;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {

    Optional<Specialization> findByName(String name);

}
