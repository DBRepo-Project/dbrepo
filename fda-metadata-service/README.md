# Metadata Service

Conforms (partly) to OAI-PMH 2.0

THe exposed endpoint is `http://localhost:9098/api/oai` or at the gateway `http://localhost:9095/api/oai`

## Implemented

### Identify

```console
$ curl -X GET http://localhost:9095/api/oai?verb=Identify
<?xml version='1.0' encoding='UTF-8'?>
<OAI-PMH xmlns='http://www.openarchives.org/OAI/2.0/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
         xsi:schemaLocation='http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd'>
    <responseDate>2022-09-17T20:07:30Z</responseDate>
    <request verb="Identify">https://metadata-service:9098/api/oai</request>
    <Identify>
    <repositoryName>Example Repository</repositoryName>
    <baseURL>https://example.com</baseURL>
    <protocolVersion>2.0</protocolVersion>
    <adminEmail>noreply@example.com</adminEmail>
    <earliestDatestamp>2022-09-17T18:23:00Z</earliestDatestamp>
    <deletedRecord>persistent</deletedRecord>
    <granularity>YYYY-MM-DDThh:mm:ssZ</granularity>
</Identify>
</OAI-PMH>(
```

### ListIdentifiers

```console
$ curl -X GET http://localhost:9095/api/oai?verb=ListIdentifiers
```