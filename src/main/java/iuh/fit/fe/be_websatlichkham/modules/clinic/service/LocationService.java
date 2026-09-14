package iuh.fit.fe.be_websatlichkham.modules.clinic.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Location;

public interface LocationService {

    Location getById(Long id);

    List<Location> findAll();

}
