package at.tuwien.gateway;

import at.tuwien.api.ror.RorDto;
import at.tuwien.exception.RorNotFoundException;

public interface RorGateway {

    RorDto findById(String id) throws RorNotFoundException;
}
