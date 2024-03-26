from __future__ import annotations

from dataclasses import field
from enum import Enum
import datetime
from typing import List, Optional, Any, Annotated
from pydantic import BaseModel, ConfigDict, PlainSerializer

Timestamp = Annotated[
    datetime.datetime, PlainSerializer(lambda v: v.isoformat(timespec='milliseconds'), return_type=str)
]

class ImageDate(BaseModel):
    id: int
    example: str
    database_format: str
    unix_format: str
    has_time: bool
    created_at: Timestamp


class Image(BaseModel):
    id: int
    registry: str
    name: str
    version: str
    dialect: str
    driver_class: str
    jdbc_method: str
    default_port: int
    date_formats: Optional[List[ImageDate]] = field(default_factory=list)


class ImageBrief(BaseModel):
    id: int
    name: str
    version: str
    jdbc_method: str


class CreateDatabase(BaseModel):
    name: str
    container_id: int
    is_public: bool


class CreateUser(BaseModel):
    username: str
    email: str
    password: str


class UpdateUser(BaseModel):
    firstname: Optional[str] = None
    lastname: Optional[str] = None
    affiliation: Optional[str] = None
    orcid: Optional[str] = None


class UserBrief(BaseModel):
    id: str
    username: str
    name: str
    orcid: str
    qualified_name: str
    given_name: str
    family_name: str


class Container(BaseModel):
    id: int
    name: str
    internal_name: str
    host: str
    port: int
    image: Image
    created: Timestamp
    sidecar_host: str
    sidecar_port: int
    ui_host: Optional[str] = None
    ui_port: Optional[int] = None


class ContainerBrief(BaseModel):
    id: int
    name: str
    image: ImageBrief
    created: Timestamp
    internal_name: str
    running: Optional[bool] = None
    hash: Optional[str] = None


class ColumnBrief(BaseModel):
    id: int
    name: str
    alias: str
    database_id: int
    table_id: int
    internal_name: str
    column_type: ColumnType


class TableBrief(BaseModel):
    id: int
    name: str
    description: str
    owner: UserBrief
    columns: List[ColumnBrief]
    internal_name: str
    is_versioned: bool


class UserAttributes(BaseModel):
    theme: str
    orcid: Optional[str] = None
    affiliation: Optional[str] = None


class User(BaseModel):
    id: str
    username: str
    attributes: UserAttributes
    qualified_name: Optional[str] = None
    given_name: Optional[str] = None
    family_name: Optional[str] = None
    name: Optional[str] = None


class UpdateUserTheme(BaseModel):
    theme: str


class UpdateUserPassword(BaseModel):
    password: str


class UserBrief(BaseModel):
    id: str
    username: str
    name: Optional[str] = None
    orcid: Optional[str] = None
    qualified_name: Optional[str] = None
    given_name: Optional[str] = None
    family_name: Optional[str] = None


class AccessType(str, Enum):
    """
    Enumeration of database access.
    """
    READ = "read"
    """The user can read all data."""

    WRITE_OWN = "write_own"
    """The user can write into self-owned tables and read all data."""

    WRITE_ALL = "write_all"
    """The user can write in all tables and read all data."""


class ColumnType(str, Enum):
    """
    Enumeration of table column data types.
    """
    CHAR = "char"
    VARCHAR = "varchar"
    BINARY = "binary"
    VARBINARY = "varbinary"
    TINYBLOB = "tinyblob"
    TINYTEXT = "tinytext"
    TEXT = "text"
    BLOB = "blob"
    MEDIUMTEXT = "mediumtext"
    MEDIUMBLOB = "mediumblob"
    LONGTEXT = "longtext"
    LONGBLOB = "longblob"
    ENUM = "enum"
    SET = "set"
    BIT = "bit"
    TINYINT = "tinyint"
    BOOL = "bool"
    SMALLINT = "smallint"
    MEDIUMINT = "mediumint"
    INT = "int"
    BIGINT = "bigint"
    FLOAT = "float"
    DOUBLE = "double"
    DECIMAL = "decimal"
    DATE = "date"
    DATETIME = "datetime"
    TIMESTAMP = "timestamp"
    TIME = "time"
    YEAR = "year"


class Language(str, Enum):
    """
    Enumeration of languages.
    """
    AB = "ab"
    AA = "aa"
    AF = "af"
    AK = "ak"
    SQ = "sq"
    AM = "am"
    AR = "ar"
    AN = "an"
    HY = "hy"
    AS = "as"
    AV = "av"
    AE = "ae"
    AY = "ay"
    AZ = "az"
    BM = "bm"
    BA = "ba"
    EU = "eu"
    BE = "be"
    BN = "bn"
    BH = "bh"
    BI = "bi"
    BS = "bs"
    BR = "br"
    BG = "bg"
    MY = "my"
    CA = "ca"
    KM = "km"
    CH = "ch"
    CE = "ce"
    NY = "ny"
    ZH = "zh"
    CU = "cu"
    CV = "cv"
    KW = "kw"
    CO = "co"
    CR = "cr"
    HR = "hr"
    CS = "cs"
    DA = "da"
    DV = "dv"
    NL = "nl"
    DZ = "dz"
    EN = "en"
    EO = "eo"
    ET = "et"
    EE = "ee"
    FO = "fo"
    FJ = "fj"
    FI = "fi"
    FR = "fr"
    FF = "ff"
    GD = "gd"
    GL = "gl"
    LG = "lg"
    KA = "ka"
    DE = "de"
    KI = "ki"
    EL = "el"
    KL = "kl"
    GN = "gn"
    GU = "gu"
    HT = "ht"
    HA = "ha"
    HE = "he"
    HZ = "hz"
    HI = "hi"
    HO = "ho"
    HU = "hu"
    IS = "is"
    IO = "io"
    IG = "ig"
    ID = "id"
    IA = "ia"
    IE = "ie"
    IU = "iu"
    IK = "ik"
    GA = "ga"
    IT = "it"
    JA = "ja"
    JV = "jv"
    KN = "kn"
    KR = "kr"
    KS = "ks"
    KK = "kk"
    RW = "rw"
    KV = "kv"
    KG = "kg"
    KO = "ko"
    KJ = "kj"
    KU = "ku"
    KY = "ky"
    LO = "lo"
    LA = "la"
    LV = "lv"
    LB = "lb"
    LI = "li"
    LN = "ln"
    LT = "lt"
    LU = "lu"
    MK = "mk"
    MG = "mg"
    MS = "ms"
    ML = "ml"
    MT = "mt"
    GV = "gv"
    MI = "mi"
    MR = "mr"
    MH = "mh"
    RO = "ro"
    MN = "mn"
    NA = "na"
    NV = "nv"
    ND = "nd"
    NG = "ng"
    NE = "ne"
    SE = "se"
    NO = "no"
    NB = "nb"
    NN = "nn"
    II = "ii"
    OC = "oc"
    OJ = "oj"
    OR = "or"
    OM = "om"
    OS = "os"
    PI = "pi"
    PA = "pa"
    PS = "ps"
    FA = "fa"
    PL = "pl"
    PT = "pt"
    QU = "qu"
    RM = "rm"
    RN = "rn"
    RU = "ru"
    SM = "sm"
    SG = "sg"
    SA = "sa"
    SC = "sc"
    SR = "sr"
    SN = "sn"
    SD = "sd"
    SI = "si"
    SK = "sk"
    SL = "sl"
    SO = "so"
    ST = "st"
    NR = "nr"
    ES = "es"
    SU = "su"
    SW = "sw"
    SS = "ss"
    SV = "sv"
    TL = "tl"
    TY = "ty"
    TG = "tg"
    TA = "ta"
    TT = "tt"
    TE = "te"
    TH = "th"
    BO = "bo"
    TI = "ti"
    TO = "to"
    TS = "ts"
    TN = "tn"
    TR = "tr"
    TK = "tk"
    TW = "tw"
    UG = "ug"
    UK = "uk"
    UR = "ur"
    UZ = "uz"
    VE = "ve"
    VI = "vi"
    VO = "vo"
    WA = "wa"
    CY = "cy"
    FY = "fy"
    WO = "wo"
    XH = "xh"
    YI = "yi"
    YO = "yo"
    ZA = "za"
    ZU = "zu"


class DatabaseAccess(BaseModel):
    type: AccessType
    user: User
    created: Timestamp


class CreateAccess(BaseModel):
    type: AccessType


class UpdateAccess(BaseModel):
    type: AccessType


class IdentifierTitle(BaseModel):
    """
    Title of an identifier. See external documentation: https://support.datacite.org/docs/datacite-metadata-schema-v44-mandatory-properties#3-title.
    """
    id: int
    title: str
    language: Optional[Language] = None
    type: Optional[TitleType] = None


class CreateIdentifierTitle(BaseModel):
    title: str
    language: Optional[Language] = None
    type: Optional[TitleType] = None


class IdentifierDescription(BaseModel):
    id: int
    description: str
    language: Optional[Language] = None
    type: Optional[DescriptionType] = None


class CreateIdentifierDescription(BaseModel):
    description: str
    language: Optional[Language] = None
    type: Optional[DescriptionType] = None


class IdentifierFunder(BaseModel):
    id: int
    funder_name: str
    funder_identifier: Optional[str] = None
    funder_identifier_type: Optional[str] = None
    scheme_uri: Optional[str] = None
    award_number: Optional[str] = None
    award_title: Optional[str] = None


class CreateIdentifierFunder(BaseModel):
    funder_name: str
    funder_identifier: Optional[str] = None
    funder_identifier_type: Optional[str] = None
    scheme_uri: Optional[str] = None
    award_number: Optional[str] = None
    award_title: Optional[str] = None


class License(BaseModel):
    identifier: str
    uri: str
    description: str


class CreateData(BaseModel):
    data: dict


class UpdateData(BaseModel):
    data: dict
    keys: dict


class DeleteData(BaseModel):
    keys: dict


class Import(BaseModel):
    location: str
    separator: str
    quote: Optional[str] = None
    skip_lines: Optional[int] = None
    false_element: Optional[bool] = None
    true_element: Optional[bool] = None
    null_element: Optional[str] = None
    line_termination: Optional[str] = None


class UpdateColumn(BaseModel):
    concept_uri: Optional[str] = None
    unit_uri: Optional[str] = None


class ModifyVisibility(BaseModel):
    is_public: bool


class ModifyOwner(BaseModel):
    id: str


class CreateTable(BaseModel):
    name: str
    constraints: CreateTableConstraints
    columns: List[CreateTableColumn] = field(default_factory=list)
    description: Optional[str] = None


class CreateTableColumn(BaseModel):
    name: str
    type: ColumnType
    primary_key: bool
    null_allowed: bool
    index_length: Optional[int] = None
    size: Optional[int] = None
    d: Optional[int] = None
    dfid: Optional[int] = None
    enums: Optional[List[str]] = None
    sets: Optional[List[str]] = None


class CreateTableConstraints(BaseModel):
    uniques: List[List[str]] = field(default_factory=list)
    checks: List[str] = field(default_factory=list)
    foreign_keys: List[CreateForeignKey] = field(default_factory=list)


class IdentifierCreator(BaseModel):
    id: int
    creator_name: str
    firstname: Optional[str] = None
    lastname: Optional[str] = None
    affiliation: Optional[str] = None
    name_type: Optional[str] = None
    name_identifier: Optional[str] = None
    name_identifier_scheme: Optional[str] = None
    name_identifier_scheme_uri: Optional[str] = None
    affiliation_identifier: Optional[str] = None
    affiliation_identifier_scheme: Optional[str] = None
    affiliation_identifier_scheme_uri: Optional[str] = None


class CreateIdentifierCreator(BaseModel):
    creator_name: str
    firstname: Optional[str] = None
    lastname: Optional[str] = None
    affiliation: Optional[str] = None
    name_type: Optional[str] = None
    name_identifier: Optional[str] = None
    name_identifier_scheme: Optional[str] = None
    name_identifier_scheme_uri: Optional[str] = None
    affiliation_identifier: Optional[str] = None
    affiliation_identifier_scheme: Optional[str] = None
    affiliation_identifier_scheme_uri: Optional[str] = None


class RelatedIdentifier(BaseModel):
    id: int
    value: str
    type: RelatedIdentifierType
    relation: RelatedIdentifierRelation


class CreateRelatedIdentifier(BaseModel):
    value: str
    type: RelatedIdentifierType
    relation: RelatedIdentifierRelation


class CreateIdentifier(BaseModel):
    type: IdentifierType
    creators: List[CreateIdentifierCreator]
    publication_year: int
    titles: Optional[List[CreateIdentifierTitle]] = field(default_factory=list)
    descriptions: Optional[List[CreateIdentifierDescription]] = field(default_factory=list)
    funders: Optional[List[CreateIdentifierFunder]] = field(default_factory=list)
    doi: Optional[str] = None
    publisher: Optional[str] = None
    language: Optional[str] = None
    licenses: Optional[List[License]] = field(default_factory=list)
    database_id: Optional[int] = None
    query_id: Optional[int] = None
    table_id: Optional[int] = None
    view_id: Optional[int] = None
    query: Optional[str] = None
    query_normalized: Optional[str] = None
    execution: Optional[str] = None
    related_identifiers: Optional[List[CreateRelatedIdentifier]] = field(default_factory=list)
    result_hash: Optional[str] = None
    result_number: Optional[int] = None
    publication_day: Optional[int] = None
    publication_month: Optional[int] = None


class Identifier(BaseModel):
    id: int
    type: IdentifierType
    creators: List[IdentifierCreator]
    created: Timestamp
    publication_year: int
    last_modified: Timestamp
    titles: Optional[List[IdentifierTitle]] = field(default_factory=list)
    descriptions: Optional[List[IdentifierDescription]] = field(default_factory=list)
    funders: Optional[List[IdentifierFunder]] = field(default_factory=list)
    doi: Optional[str] = None
    publisher: Optional[str] = None
    language: Optional[str] = None
    licenses: Optional[List[License]] = field(default_factory=list)
    database_id: Optional[int] = None
    query_id: Optional[int] = None
    table_id: Optional[int] = None
    view_id: Optional[int] = None
    query: Optional[str] = None
    query_normalized: Optional[str] = None
    execution: Optional[str] = None
    related_identifiers: Optional[List[RelatedIdentifier]] = field(default_factory=list)
    result_hash: Optional[str] = None
    result_number: Optional[int] = None
    publication_day: Optional[int] = None
    publication_month: Optional[int] = None


class View(BaseModel):
    id: int
    database_id: int
    name: str
    query: str
    query_hash: str
    created: Timestamp
    creator: User
    internal_name: str
    is_public: bool
    initial_view: bool
    last_modified: Timestamp
    identifiers: List[Identifier] = field(default_factory=list)


class CreateView(BaseModel):
    name: str
    query: str
    is_public: bool


class Result(BaseModel):
    result: Any
    headers: Any
    id: Optional[int] = None


class ViewBrief(BaseModel):
    id: int
    database_id: int
    name: str
    identifier: List[Identifier]
    query: str
    query_hash: str
    created: Timestamp
    creator: User
    internal_name: str
    is_public: bool
    initial_view: bool
    last_modified: Timestamp


class ColumnBrief(BaseModel):
    id: int
    name: str
    alias: str
    database_id: int
    table_id: int
    internal_name: str
    column_type: str


class Concept(BaseModel):
    id: int
    uri: str
    created: Timestamp
    columns: List[ColumnBrief] = field(default_factory=list)
    name: Optional[str] = None
    description: Optional[str] = None


class DatatypeAnalysis(BaseModel):
    separator: str
    columns: dict[str, ColumnType]
    line_termination: Optional[str] = None


class KeyAnalysis(BaseModel):
    keys: dict[str, int]


class ColumnStatistic(BaseModel):
    val_min: float
    val_max: float
    mean: float
    median: float
    std_dev: float


class TableStatistics(BaseModel):
    columns: dict[str, ColumnStatistic]


class Unit(BaseModel):
    id: int
    uri: str
    created: Timestamp
    columns: List[ColumnBrief] = field(default_factory=list)
    name: Optional[str] = None
    description: Optional[str] = None


class ExecuteQuery(BaseModel):
    statement: str
    timestamp: Timestamp


class TitleType(str, Enum):
    """
    Enumeration of identifier title types.
    """
    ALTERNATIVE_TITLE = "AlternativeTitle"
    SUBTITLE = "Subtitle"
    TRANSLATED_TITLE = "TranslatedTitle"
    OTHER = "Other"


class RelatedIdentifierType(str, Enum):
    """
    Enumeration of related identifier types.
    """
    DOI = "DOI"
    URL = "URL"
    URN = "URN"
    ARK = "ARK"
    ARXIV = "arXiv"
    BIBCODE = "bibcode"
    EAN13 = "EAN13"
    EISSN = "EISSN"
    HANDLE = "Handle"
    IGSN = "IGSN"
    ISBN = "ISBN"
    ISTC = "ISTC"
    LISSN = "LISSN"
    LSID = "LSID"
    PMID = "PMID"
    PURL = "PURL"
    UPC = "UPC"
    W3ID = "w3id"


class RelatedIdentifierRelation(str, Enum):
    """
    Enumeration of related identifier types.
    """
    IS_CITED_BY = "IsCitedBy"
    CITES = "Cites"
    IS_SUPPLEMENT_TO = "IsSupplementTo"
    IS_SUPPLEMENTED_BY = "IsSupplementedBy"
    IS_CONTINUED_BY = "IsContinuedBy"
    CONTINUES = "Continues"
    IS_DESCRIBED_BY = "IsDescribedBy"
    DESCRIBES = "Describes"
    HAS_METADATA = "HasMetadata"
    IS_METADATA_FOR = "IsMetadataFor"
    HAS_VERSION = "HasVersion"
    IS_VERSION_OF = "IsVersionOf"
    IS_NEW_VERSION_OF = "IsNewVersionOf"
    IS_PREVIOUS_VERSION_OF = "IsPreviousVersionOf"
    IS_PART_OF = "IsPartOf"
    HAS_PART = "HasPart"
    IS_PUBLISHED_IN = "IsPublishedIn"
    IS_REFERENCED_BY = "IsReferencedBy"
    REFERENCES = "References"
    IS_DOCUMENTED_BY = "IsDocumentedBy"
    DOCUMENTS = "Documents"
    IS_COMPILED_BY = "IsCompiledBy"
    COMPILES = "Compiles"
    IS_VARIANT_FORM_OF = "IsVariantFormOf"
    IS_ORIGINAL_FORM_OF = "IsOriginalFormOf"
    IS_IDENTICAL_TO = "IsIdenticalTo"
    IS_REVIEWED_BY = "IsReviewedBy"
    REVIEWS = "Reviews"
    IS_DERIVED_FROM = "IsDerivedFrom"
    IS_SOURCE_OF = "IsSourceOf"
    IS_REQUIRED_BY = "IsRequiredBy"
    REQUIRES = "Requires"
    IS_OBSOLETED_BY = "IsObsoletedBy"
    OBSOLETES = "Obsoletes"


class DescriptionType(str, Enum):
    """
    Enumeration of identifier description types.
    """
    ABSTRACT = "Abstract"
    METHODS = "Methods"
    SERIES_INFORMATION = "SeriesInformation"
    TABLE_OF_CONTENTS = "TableOfContents"
    TECHNICAL_INFO = "TechnicalInfo"
    OTHER = "Other"


class QueryType(str, Enum):
    """
    Enumeration of query types.
    """
    VIEW = "view"
    """The query was executed as part of a view."""

    QUERY = "query"
    """The query was executed as subset."""


class IdentifierType(str, Enum):
    """
    Enumeration of identifier types.
    """
    VIEW = "view"
    """The identifier is identifying a view."""

    SUBSET = "subset"
    """The identifier is identifying a subset."""

    DATABASE = "database"
    """The identifier is identifying a database."""

    TABLE = "table"
    """The identifier is identifying a table."""


class IdentifierType(str, Enum):
    """
    Enumeration of identifier types.
    """
    TABLE = "table"
    """The identifier identifies a table."""

    DATABASE = "database"
    """The identifier identifies a database."""

    VIEW = "view"
    """The identifier identifies a view."""

    SUBSET = "subset"
    """The identifier identifies a subset."""


class Query(BaseModel):
    id: int
    creator: User
    execution: Timestamp
    query: str
    type: QueryType
    created: Timestamp
    database_id: int
    query_hash: str
    is_persisted: bool
    result_hash: str
    query_normalized: str
    last_modified: Timestamp
    result_number: Optional[int] = None
    identifiers: List[Identifier] = field(default_factory=list)


class UpdateQuery(BaseModel):
    persist: bool


class Column(BaseModel):
    id: int
    name: str
    database_id: int
    table_id: int
    internal_name: str
    auto_generated: bool
    is_primary_key: bool
    column_type: ColumnType
    is_public: bool
    is_null_allowed: bool
    alias: Optional[str] = None
    size: Optional[int] = None
    d: Optional[int] = None
    mean: Optional[float] = None
    median: Optional[float] = None
    concept: Optional[Concept] = None
    unit: Optional[Unit] = None
    enums: Optional[List[str]] = field(default_factory=list)
    sets: Optional[List[str]] = field(default_factory=list)
    date_format: Optional[ImageDate] = None
    index_length: Optional[int] = None
    length: Optional[int] = None
    data_length: Optional[int] = None
    max_data_length: Optional[int] = None
    num_rows: Optional[int] = None
    val_min: Optional[float] = None
    val_max: Optional[float] = None
    std_dev: Optional[float] = None


class Table(BaseModel):
    id: int
    database_id: int
    name: str
    creator: User
    owner: User
    created: Timestamp
    columns: List[Column]
    constraints: Constraints
    internal_name: str
    is_versioned: bool
    created_by: str
    queue_name: str
    routing_key: str
    is_public: bool
    identifiers: Optional[List[Identifier]] = field(default_factory=list)
    description: Optional[str] = None
    queue_type: Optional[str] = None
    num_rows: Optional[int] = None
    data_length: Optional[int] = None
    max_data_length: Optional[int] = None
    avg_row_length: Optional[int] = None


class Database(BaseModel):
    id: int
    name: str
    creator: User
    owner: User
    contact: User
    created: Timestamp
    exchange_name: str
    internal_name: str
    is_public: bool
    container: Container
    identifiers: Optional[List[Identifier]] = field(default_factory=list)
    subsets: Optional[List[Identifier]] = field(default_factory=list)
    description: Optional[str] = None
    tables: Optional[List[Table]] = field(default_factory=list)
    views: Optional[List[View]] = field(default_factory=list)
    image: Optional[str] = None
    accesses: Optional[List[DatabaseAccess]] = field(default_factory=list)
    exchange_type: Optional[str] = None


class Unique(BaseModel):
    uid: int
    table: Table
    columns: List[Column]


class ForeignKey(BaseModel):
    name: str
    columns: List[Column]
    referenced_table: Table
    referenced_columns: List[Column]
    on_update: Optional[str] = None
    on_delete: Optional[str] = None


class CreateForeignKey(BaseModel):
    columns: List[Column]
    referenced_table: Table
    referenced_columns: List[Column]
    on_update: Optional[str] = None
    on_delete: Optional[str] = None


class Constraints(BaseModel):
    uniques: Optional[List[Unique]] = None
    foreign_keys: Optional[List[ForeignKey]] = None
    checks: Optional[List[str]] = None
