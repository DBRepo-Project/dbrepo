# RelatedIdentifierDto

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Long** |  | 
**value** | **String** |  | 
**type** | [**TypeEnum**](#TypeEnum) |  |  [optional]
**relation** | [**RelationEnum**](#RelationEnum) |  |  [optional]
**created** | [**OffsetDateTime**](OffsetDateTime.md) |  | 
**lastModified** | [**OffsetDateTime**](OffsetDateTime.md) |  |  [optional]

<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
DOI | &quot;DOI&quot;
URL | &quot;URL&quot;
URN | &quot;URN&quot;
ARK | &quot;ARK&quot;
ARXIV | &quot;arXiv&quot;
BIBCODE | &quot;bibcode&quot;
EAN13 | &quot;EAN13&quot;
EISSN | &quot;EISSN&quot;
HANDLE | &quot;Handle&quot;
IGSN | &quot;IGSN&quot;
ISBN | &quot;ISBN&quot;
ISTC | &quot;ISTC&quot;
LISSN | &quot;LISSN&quot;
LSID | &quot;LSID&quot;
PMID | &quot;PMID&quot;
PURL | &quot;PURL&quot;
UPC | &quot;UPC&quot;
W3ID | &quot;w3id&quot;

<a name="RelationEnum"></a>
## Enum: RelationEnum
Name | Value
---- | -----
ISCITEDBY | &quot;IsCitedBy&quot;
CITES | &quot;Cites&quot;
ISSUPPLEMENTTO | &quot;IsSupplementTo&quot;
ISSUPPLEMENTEDBY | &quot;IsSupplementedBy&quot;
ISCONTINUEDBY | &quot;IsContinuedBy&quot;
CONTINUES | &quot;Continues&quot;
ISDESCRIBEDBY | &quot;IsDescribedBy&quot;
DESCRIBES | &quot;Describes&quot;
HASMETADATA | &quot;HasMetadata&quot;
ISMETADATAFOR | &quot;IsMetadataFor&quot;
HASVERSION | &quot;HasVersion&quot;
ISVERSIONOF | &quot;IsVersionOf&quot;
ISNEWVERSIONOF | &quot;IsNewVersionOf&quot;
ISPREVIOUSVERSIONOF | &quot;IsPreviousVersionOf&quot;
ISPARTOF | &quot;IsPartOf&quot;
HASPART | &quot;HasPart&quot;
ISPUBLISHEDIN | &quot;IsPublishedIn&quot;
ISREFERENCEDBY | &quot;IsReferencedBy&quot;
REFERENCES | &quot;References&quot;
ISDOCUMENTEDBY | &quot;IsDocumentedBy&quot;
DOCUMENTS | &quot;Documents&quot;
ISCOMPILEDBY | &quot;IsCompiledBy&quot;
COMPILES | &quot;Compiles&quot;
ISVARIANTFORMOF | &quot;IsVariantFormOf&quot;
ISORIGINALFORMOF | &quot;IsOriginalFormOf&quot;
ISIDENTICALTO | &quot;IsIdenticalTo&quot;
ISREVIEWEDBY | &quot;IsReviewedBy&quot;
REVIEWS | &quot;Reviews&quot;
ISDERIVEDFROM | &quot;IsDerivedFrom&quot;
ISSOURCEOF | &quot;IsSourceOf&quot;
ISREQUIREDBY | &quot;IsRequiredBy&quot;
REQUIRES | &quot;Requires&quot;
ISOBSOLETEDBY | &quot;IsObsoletedBy&quot;
OBSOLETES | &quot;Obsoletes&quot;
