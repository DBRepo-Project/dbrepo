package at.tuwien.service.impl;

import at.tuwien.entities.auth.Realm;
import at.tuwien.exception.RealmNotFoundException;
import at.tuwien.repository.jpa.RealmRepository;
import at.tuwien.service.RealmService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class RealmServiceImpl implements RealmService {

    final RealmRepository realmRepository;

    @Autowired
    public RealmServiceImpl(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    @Override
    public Realm find(String name) throws RealmNotFoundException {
        final Optional<Realm> optional = realmRepository.findByName(name);
        if (optional.isEmpty()) {
            log.error("Failed to find realm with name '{}'", name);
            throw new RealmNotFoundException("Failed to find realm");
        }
        return optional.get();
    }

    @Override
    public Realm update(String name) throws RealmNotFoundException {
        final Realm realm = find("master");
        realm.setSslRequired("NONE");
        final Realm entity = realmRepository.save(realm);
        log.info("Disabled SSL for realm with name '{}'", name);
        return entity;
    }

}
