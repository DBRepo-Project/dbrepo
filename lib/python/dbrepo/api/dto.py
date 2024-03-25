from __future__ import annotations

import datetime
from enum import Enum
from typing import List, Optional
from dataclasses import dataclass, field
from dataclasses_json import dataclass_json


@dataclass_json
@dataclass(init=True, eq=True)
class ImageDate:
    id: int
    example: str
    database_format: str
    unix_format: str
    has_time: bool
    created_at: str


@dataclass_json
@dataclass(init=True, eq=True)
class Image:
    id: int
    registry: str
    name: str
    version: str
    dialect: str
    driver_class: str
    jdbc_method: str
    default_port: int
    date_formats: Optional[List[ImageDate]] = field(default_factory=list)


@dataclass_json
@dataclass(init=True, eq=True)
class ImageBrief:
    id: int
    name: str
    version: str
    jdbc_method: str


@dataclass_json
@dataclass(init=True, eq=True)
class CreateDatabase:
    name: int
    container_id: int
    is_public: bool


@dataclass_json
@dataclass(init=True, eq=True)
class CreateUser:
    username: str
    email: str
    password: str


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateUser:
    firstname: str
    lastname: str
    affiliation: str
    orcid: str


@dataclass_json
@dataclass(init=True, eq=True)
class UserBrief:
    id: str
    username: str
    name: str
    orcid: str
    qualified_name: str
    given_name: str
    family_name: str


@dataclass_json
@dataclass(init=True, eq=True)
class Container:
    id: int
    name: str
    host: str
    port: int
    image: Image
    created: str
    internal_name: str
    sidecar_host: str
    sidecar_port: int
    ui_host: Optional[str] = None
    ui_port: Optional[int] = None


@dataclass_json
@dataclass(init=True, eq=True)
class ContainerBrief:
    id: int
    name: str
    image: ImageBrief
    created: str
    internal_name: str
    running: Optional[bool] = None
    hash: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class ColumnBrief:
    id: int
    name: str
    alias: str
    database_id: int
    table_id: int
    internal_name: str
    column_type: ColumnType


@dataclass_json
@dataclass(init=True, eq=True)
class TableBrief:
    id: int
    name: str
    description: str
    owner: UserBrief
    columns: List[ColumnBrief]
    internal_name: str
    is_versioned: bool


@dataclass_json
@dataclass(init=True, eq=True)
class UserAttributes:
    theme: str
    orcid: Optional[str] = None
    affiliation: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class User:
    id: str
    username: str
    attributes: UserAttributes
    qualified_name: Optional[str] = None
    given_name: Optional[str] = None
    family_name: Optional[str] = None
    name: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateUserTheme:
    theme: str


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateUserPassword:
    password: str


@dataclass_json
@dataclass(init=True, eq=True)
class UserBrief:
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


@dataclass_json
@dataclass(init=True, eq=True)
class DatabaseAccess:
    type: AccessType
    user: User
    created: str


@dataclass_json
@dataclass(init=True, eq=True)
class CreateAccess:
    type: AccessType


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateAccess:
    type: AccessType


@dataclass_json
@dataclass(init=True, eq=True)
class IdentifierTitle:
    id: int
    title: str
    language: Optional[Language] = None
    type: Optional[TitleType] = None


@dataclass_json
@dataclass(init=True, eq=True)
class CreateIdentifierTitle:
    title: str
    language: Optional[Language] = None
    type: Optional[TitleType] = None


@dataclass_json
@dataclass(init=True, eq=True)
class IdentifierDescription:
    id: int
    description: str
    language: Optional[Language] = None
    type: Optional[DescriptionType] = None


@dataclass_json
@dataclass(init=True, eq=True)
class CreateIdentifierDescription:
    description: str
    language: Optional[Language] = None
    type: Optional[DescriptionType] = None


@dataclass_json
@dataclass(init=True, eq=True)
class IdentifierFunder:
    id: int
    funder_name: str
    funder_identifier: Optional[str] = None
    funder_identifier_type: Optional[str] = None
    scheme_uri: Optional[str] = None
    award_number: Optional[str] = None
    award_title: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class CreateIdentifierFunder:
    funder_name: str
    funder_identifier: Optional[str] = None
    funder_identifier_type: Optional[str] = None
    scheme_uri: Optional[str] = None
    award_number: Optional[str] = None
    award_title: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class License:
    identifier: str
    uri: str
    description: str


@dataclass_json
@dataclass(init=True, eq=True)
class CreateData:
    data: dict


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateData:
    data: dict
    keys: dict


@dataclass_json
@dataclass(init=True, eq=True)
class DeleteData:
    keys: dict


@dataclass_json
@dataclass(init=True, eq=True)
class Import:
    location: str
    separator: str
    quote: Optional[str] = None
    skip_lines: Optional[int] = None
    false_element: Optional[bool] = None
    true_element: Optional[bool] = None
    null_element: Optional[str] = None
    line_termination: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateColumn:
    concept_uri: Optional[str] = None
    unit_uri: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class ModifyVisibility:
    is_public: bool


@dataclass_json
@dataclass(init=True, eq=True)
class ModifyOwner:
    id: str


@dataclass_json
@dataclass(init=True, eq=True)
class CreateTable:
    name: str
    constraints: CreateTableConstraints
    columns: List[CreateTableColumn] = field(default_factory=list)
    description: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class CreateTableColumn:
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


@dataclass_json
@dataclass(init=True, eq=True)
class CreateTableConstraints:
    uniques: List[List[str]] = field(default_factory=list)
    checks: List[str] = field(default_factory=list)
    foreign_keys: List[CreateForeignKey] = field(default_factory=list)


@dataclass_json
@dataclass(init=True, eq=True)
class IdentifierCreator:
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


@dataclass_json
@dataclass(init=True, eq=True)
class CreateIdentifierCreator:
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


@dataclass_json
@dataclass(init=True, eq=True)
class RelatedIdentifier:
    id: int
    value: str
    type: RelatedIdentifierType
    relation: RelatedIdentifierRelation


@dataclass_json
@dataclass(init=True, eq=True)
class CreateRelatedIdentifier:
    value: str
    type: RelatedIdentifierType
    relation: RelatedIdentifierRelation


@dataclass_json
@dataclass(init=True, eq=True)
class CreateIdentifier:
    type: IdentifierType
    creators: List[CreateIdentifierCreator]
    publication_year: int
    titles: Optional[List[CreateIdentifierTitle]] = field(default_factory=list)
    descriptions: Optional[List[CreateIdentifierTitle]] = field(default_factory=list)
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


@dataclass_json
@dataclass(init=True, eq=True)
class Identifier:
    id: int
    type: IdentifierType
    creators: List[IdentifierCreator]
    created: str
    publication_year: int
    last_modified: str
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


@dataclass_json
@dataclass(init=True, eq=True)
class View:
    id: int
    database_id: int
    name: str
    query: str
    query_hash: str
    created: str
    creator: User
    internal_name: str
    is_public: bool
    initial_view: bool
    last_modified: str
    identifiers: List[Identifier] = field(default_factory=list)


@dataclass_json
@dataclass(init=True, eq=True)
class CreateView:
    name: str
    query: str
    is_public: bool


@dataclass_json
@dataclass(init=True, eq=True)
class Result:
    result: List[any]
    headers: List[str]
    id: Optional[int] = None


@dataclass_json
@dataclass(init=True, eq=True)
class ViewBrief:
    id: int
    database_id: int
    name: str
    identifier: List[Identifier]
    query: str
    query_hash: str
    created: str
    creator: User
    internal_name: str
    is_public: bool
    initial_view: bool
    last_modified: str


@dataclass_json
@dataclass(init=True, eq=True)
class ColumnBrief:
    id: int
    name: str
    alias: str
    database_id: int
    table_id: int
    internal_name: str
    column_type: str


@dataclass_json
@dataclass(init=True, eq=True)
class Concept:
    id: int
    uri: str
    created: str
    columns: List[ColumnBrief] = field(default_factory=list)
    name: Optional[str] = None
    description: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class DatatypeAnalysis:
    separator: str
    columns: dict[str, ColumnType]
    line_termination: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class KeyAnalysis:
    keys: dict[str, int]


@dataclass_json
@dataclass(init=True, eq=True)
class ColumnStatistic:
    val_min: float
    val_max: float
    mean: float
    median: float
    std_dev: float


@dataclass_json
@dataclass(init=True, eq=True)
class TableStatistics:
    columns: dict[str, ColumnStatistic]


@dataclass_json
@dataclass(init=True, eq=True)
class Unit:
    id: int
    uri: str
    created: str
    columns: List[ColumnBrief] = field(default_factory=list)
    name: Optional[str] = None
    description: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class ExecuteQuery:
    statement: str
    timestamp: datetime.datetime


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


@dataclass_json
@dataclass(init=True, eq=True)
class IdentifierTitle:
    """
    Title of an identifier. See external documentation: https://support.datacite.org/docs/datacite-metadata-schema-v44-mandatory-properties#3-title.
    """
    id: int
    title: str
    language: Optional[str] = None
    type: Optional[str] = None


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


@dataclass_json
@dataclass(init=True, eq=True)
class Query:
    id: int
    creator: User
    execution: str
    query: str
    type: QueryType
    created: str
    database_id: int
    query_hash: str
    is_persisted: bool
    result_hash: str
    query_normalized: str
    last_modified: str
    result_number: Optional[int] = None
    identifiers: List[Identifier] = field(default_factory=list)


@dataclass_json
@dataclass(init=True, eq=True)
class UpdateQuery:
    persist: bool


@dataclass_json
@dataclass(init=True, eq=True)
class Column:
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


@dataclass_json
@dataclass(init=True, eq=True)
class Table:
    id: int
    database_id: int
    name: str
    creator: User
    owner: User
    created: str
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


@dataclass_json
@dataclass(init=True, eq=True)
class Database:
    id: int
    name: str
    creator: User
    owner: User
    contact: User
    created: str
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


@dataclass_json
@dataclass(init=True, eq=True)
class Unique:
    uid: int
    table: Table
    columns: List[Column]


@dataclass_json
@dataclass(init=True, eq=True)
class ForeignKey:
    name: str
    columns: List[Column]
    referenced_table: Table
    referenced_columns: List[Column]
    on_update: Optional[str] = None
    on_delete: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class CreateForeignKey:
    columns: List[Column]
    referenced_table: Table
    referenced_columns: List[Column]
    on_update: Optional[str] = None
    on_delete: Optional[str] = None


@dataclass_json
@dataclass(init=True, eq=True)
class Constraints:
    uniques: Optional[List[Unique]] = None
    foreign_keys: Optional[List[ForeignKey]] = None
    checks: Optional[List[str]] = None
