package iuh.fit.fe.be_websatlichkham.modules.clinic.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.clinic.entity.Location;
import iuh.fit.fe.be_websatlichkham.modules.clinic.repository.LocationRepository;
import iuh.fit.fe.be_websatlichkham.modules.clinic.service.LocationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    public Location getById(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }

    @Override
    public List<Location> findAll() {
        return locationRepository.findAll(Sort.by("name"));
    }

}
