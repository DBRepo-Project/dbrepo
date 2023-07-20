package at.tuwien.service;

import at.tuwien.entities.database.View;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ViewService {
    @Transactional(readOnly = true)
    List<View> findAll();
}
