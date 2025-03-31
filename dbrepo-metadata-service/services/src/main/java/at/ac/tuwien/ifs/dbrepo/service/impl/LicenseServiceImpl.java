package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.License;
import at.ac.tuwien.ifs.dbrepo.core.exception.LicenseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.service.LicenseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRepository licenseRepository;

    public LicenseServiceImpl(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    @Override
    public List<License> findAll() {
        return licenseRepository.findAll();
    }

    @Override
    public License find(String identifier) throws LicenseNotFoundException {
        final Optional<License> license = licenseRepository.findByIdentifier(identifier);
        if (license.isEmpty()) {
            log.error("Failed to find license with identifier {}", identifier);
            throw new LicenseNotFoundException("Failed to find license with identifier " + identifier);
        }
        return license.get();
    }
}
