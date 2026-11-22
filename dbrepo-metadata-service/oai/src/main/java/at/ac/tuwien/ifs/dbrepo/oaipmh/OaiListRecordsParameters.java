package at.ac.tuwien.ifs.dbrepo.oaipmh;

import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OaiListRecordsParameters extends OaiListParameters {

    @Builder
    public OaiListRecordsParameters(String metadataPrefix, String from, String until, String set, String resumptionToken) {
        super(metadataPrefix, from, until, set, resumptionToken);
    }

    @Override
    public String verb() {
        return "ListRecords";
    }
}
