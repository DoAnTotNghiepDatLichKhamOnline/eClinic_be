package iuh.fit.fe.be_websatlichkham.modules.user.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.fe.be_websatlichkham.common.exception.ResourceNotFoundException;
import iuh.fit.fe.be_websatlichkham.modules.user.entity.Admin;
import iuh.fit.fe.be_websatlichkham.modules.user.repository.AdminRepository;
import iuh.fit.fe.be_websatlichkham.modules.user.service.AdminService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    @Override
    public Admin getById(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin", id));
    }

    @Override
    public Optional<Admin> findByUserId(Long userId) {
        return adminRepository.findByUserId(userId);
    }

}
