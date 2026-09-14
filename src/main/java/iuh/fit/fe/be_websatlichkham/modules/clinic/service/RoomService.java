package iuh.fit.fe.be_websatlichkham.modules.clinic.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Room;

public interface RoomService {

    Room getById(Long id);

    List<Room> findActiveByLocation(Long locationId);

}
