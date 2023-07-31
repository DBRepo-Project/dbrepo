package at.tuwien.service;

import at.tuwien.entities.database.View;
import at.tuwien.exception.ViewNotFoundException;

public interface ViewService {
    View findById(Long id) throws ViewNotFoundException;
}
