package at.tuwien.service;

import at.tuwien.entities.auth.Realm;
import at.tuwien.exception.RealmNotFoundException;

public interface RealmService {
    Realm find(String name) throws RealmNotFoundException;

    Realm update(String name) throws RealmNotFoundException;
}
