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

### Record formats

Use `oai_dc` to harvest all published identifiers. A record can also be harvested as
`oai_datacite` once DataCite has confirmed which repository registered its DOI.
`ListMetadataFormats&identifier=…` lists the formats for that item. A request for
an unavailable record format returns `cannotDisseminateFormat`. A list with no
eligible records returns `noRecordsMatch`. UUID-only publications are available
in Dublin Core. Requests that omit the format use Dublin Core. A direct
identifier download with `Accept: text/xml` uses DataCite for verified DOIs
and Dublin Core for other identifiers.

The metadata uses the published [Dublin Core schema](https://www.openarchives.org/OAI/2.0/oai_dc.xsd)
and the [DataCite OAI 1.1 schema](https://schema.datacite.org/oai/oai-1.1/oai.xsd).

### Existing installations

Before upgrading the metadata service, back up the metadata database and add the nullable
repository column. Run the migration on a dedicated administrative connection.

```sh
mariadb --defaults-extra-file=/private/metadata-client.cnf dbrepo < dbrepo-metadata-db/migration/datacite-oai-repository.sql
```

Existing DOIs remain available in Dublin Core. Subsequent deposits record the
repository ID returned by DataCite. For older publications, install PyMySQL in a
Python environment and preview up to 100 matches at a time. Supply the public
website URL that the repository used when it registered the DOIs.

```sh
python3 dbrepo-metadata-db/migration/backfill_datacite_oai.py \
  --defaults-file /private/metadata-client.cnf --public-url https://repo.example.org --limit 100
```

Run the same page with `--apply` to store verified matches. Continue from its
last checked ID with `--after <uuid>`. Use `--test` for the DataCite test
registry and repeat `--public-url` for former website URLs. Each update checks
the public DOI and its registered landing page against the local identifier
and updates its OAI datestamp. Unmatched or unpublished registrations keep their
current metadata. The backfill only reads from DataCite.
