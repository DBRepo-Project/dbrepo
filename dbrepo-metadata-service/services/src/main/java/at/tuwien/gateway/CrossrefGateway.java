package at.tuwien.gateway;

import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.exception.DoiNotFoundException;

public interface CrossrefGateway {
    CrossrefDto findById(String id) throws DoiNotFoundException;
}
