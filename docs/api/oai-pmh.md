---
author: Martin Weise
---

The Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) standard provides and endpoint to expose the
metadata records in DBRepo in an XML format defined by version 2.0 of the OAI-PMH protocol.

Reference:

  * [Open Archives Initiative Protocol for Metadata Harvesting](https://www.openarchives.org/OAI/openarchivesprotocol.html) (OAI-PMH)
  * [OpenAIRE Guidelines for Data Archive Managers](https://doi.org/10.5281/zenodo.6918) ("OpenAIRE Provide")

Related:

* [Metadata Service](/infrastructures/dbrepo/1.13/dev/services/metadata-service)

### Harvesting published records

Use `ListIdentifiers` for headers or `ListRecords` for records. Both require a `metadataPrefix` such as `oai_dc`. The `openaire_data` set contains all published identifiers.

To harvest records changed on 29 February 2024, request `/api/v1/oai?verb=ListRecords&metadataPrefix=oai_dc&from=2024-02-29&until=2024-02-29`. Both bounds are inclusive. Repositories with second granularity also accept UTC timestamps such as `2024-02-29T23:59:59Z`. Use the same precision for both bounds. An empty result returns the OAI error `noRecordsMatch`.

DBRepo returns the complete matching list in one response. Clients can split large harvests into date ranges.

### Harvester

We are OpenAIRE Provide 2.0-compliant:

![OpenAIRE Provide Logo](/infrastructures/dbrepo/1.13/images/logos/openaire-provide.png){ width=200 }