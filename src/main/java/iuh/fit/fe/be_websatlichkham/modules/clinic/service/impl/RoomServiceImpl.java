package iuh.fit.fe.be_websatlichkham.modules.clinic.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Room;
import iuh.fit.fe.be_websatlichkham.modules.clinic.enums.RoomStatus;
import iuh.fit.fe.be_websatlichkham.modules.clinic.repository.RoomRepository;
import iuh.fit.fe.be_websatlichkham.modules.clinic.service.RoomService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    public Room getById(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    @Override
    public List<Room> findActiveByLocation(Long locationId) {
        return roomRepository.findByLocationIdAndStatus(locationId, RoomStatus.ACTIVE);
    }

}
