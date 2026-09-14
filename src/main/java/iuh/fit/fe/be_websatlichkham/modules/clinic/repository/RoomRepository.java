package iuh.fit.fe.be_websatlichkham.modules.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Room;
import iuh.fit.fe.be_websatlichkham.modules.clinic.enums.RoomStatus;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByLocationIdAndStatus(Long locationId, RoomStatus status);

    List<Room> findByLocationIdAndSpecializationIdAndStatus(Long locationId, Long specializationId, RoomStatus status);

}
