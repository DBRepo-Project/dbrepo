package at.tuwien.service.impl;

import at.tuwien.entities.database.License;
import at.tuwien.exception.LicenseNotFoundException;
import at.tuwien.repository.mdb.LicenseRepository;
import at.tuwien.service.LicenseService;
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
