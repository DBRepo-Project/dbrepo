package at.ac.tuwien.ifs.dbrepo.oaipmh;

import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OaiListIdentifiersParameters extends OaiListParameters {

    @Builder
    public OaiListIdentifiersParameters(String metadataPrefix, String from, String until, String set, String resumptionToken) {
        super(metadataPrefix, from, until, set, resumptionToken);
    }

    @Override
    public String verb() {
        return "ListIdentifiers";
    }
}
