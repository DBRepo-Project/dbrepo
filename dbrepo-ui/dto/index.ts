interface DatabaseDto {
  id: string;
  name: string;
  creator: UserDto;
  owner: UserDto;
  contact: UserDto;
  created: Date;
  dashboard_uid: string;
  exchange_name: string;
  internal_name: string;
  is_public: boolean;
  description: string | null;
  container: ContainerBriefDto;
  identifiers: IdentifierDto[] | [];
  subsets: IdentifierDto[] | [];
  accesses: DatabaseAccessDto[];
  has_preview_image: boolean;
  identifier: IdentifierDto[];
  tables: TableDto[];
  views: ViewDto[];
  exchange_type: string | null;
}

interface UploadResponseDto {
  s3_key: string;
}

interface DatabaseCreateDto {
  name: string;
  container_id: string;
  is_public: boolean;
}

interface DatabaseAccessDto {
  user: UserDto;
  type: string;
  created: Date;
}

interface UserBriefDto {
  id: string;
  username: string;
  name: string;
  orcid: string;
  qualified_name: string;
  given_name: string;
  family_name: string;
}

interface UserDto {
  id: string;
  username: string;
  attributes: UserAttributesDto;
  name: string | null;
  qualified_name: string | null;
  given_name: string | null;
  family_name: string | null;
}

interface UserAttributesDto {
  orcid: string | null;
  affiliation: string | null;
  theme: string;
}

interface ContainerBriefDto {
  id: string;
  hash: string;
  name: string;
  image: ImageDto;
  running: boolean;
  created: Date;
  internal_name: string;
}

interface ImageDto {
  id: string;
  registry: string;
  name: string;
  version: string;
  dialect: string;
  driver_class: string;
  data_types: DataTypeDto[];
  operators: OperatorDto[];
  jdbc_method: string;
  default_port: number;
}

interface OperatorDto {
  id: string;
  image_id: string;
  display_name: string;
  documentation: string;
  value: string;
}

interface TableBriefDto {
  id: string;
  name: string;
  description: string;
  internal_name: string;
  is_versioned: boolean;
  is_public: boolean;
  is_schema_public: boolean;
  owned_by: string;
}

interface TableUpdateDto {
  description: string;
  is_public: boolean;
  is_schema_public: boolean;
}

interface ColumnBriefDto {
  id: string;
  name: string;
  alias: string;
  database_id: string;
  table_id: string;
  internal_name: string;
  column_type: string;
}

interface TableDto {
  id: string;
  database_id: string;
  name: string;
  identifiers: IdentifierDto[];
  creator: UserDto;
  owner: UserDto;
  description: string;
  created: Date;
  columns: ColumnDto[];
  constraints: ConstraintsDto;
  internal_name: string;
  is_versioned: boolean;
  created_by: string;
  queue_name: string;
  queue_type: string;
  routing_key: string;
  is_public: boolean;
  num_rows: number;
  data_length: number;
  max_data_length: number;
  avg_row_length: number;
}

interface ForeignKeyDto {
  name: string;
  columns: ColumnDto[];
  referenced_table: TableDto;
  referenced_columns: ColumnDto[];
  on_update: string | null;
  on_delete: string | null;
}

interface ConstraintsDto {
  primary_key: string[];
  uniques: UniqueDto[];
  checks: string[];
  foreign_keys: ForeignKeyDto[];
}

interface DetermineDataTypesDto {
  enum: boolean | null;
  enum_tol: number | null;
  filename: string;
  separator: string | null;
}

interface DataTypesDto {
  columns: any[];
  line_termination: string;
  separator: string;
}

interface UniqueDto {
  uid: string;
  table: TableBriefDto;
  columns: ColumnDto[];
}

interface IdentifierCreateDto {
  database_id: string;
  doi: string | null;
}

interface IdentifierSaveDto {
  id: string;
  type: string;
  doi: string | null;
  titles: IdentifierSaveTitleDto[] | [];
  descriptions: IdentifierSaveDescriptionDto[] | [];
  funders: IdentifierFunderSaveDto[] | [];
  licenses: LicenseDto[] | [];
  publisher: string | null;
  language: string | null;
  creators: CreatorSaveDto[] | [];
  database_id: string | null;
  query_id: string | null;
  view_id: string | null;
  table_id: string | null;
  publication_day: number | null;
  publication_month: number | null;
  publication_year: number | null;
  related_identifiers: RelatedIdentifierSaveDto[] | [];
}

interface IdentifierSaveTitleDto {
  title: string;
  language: string;
  type: string;
}

interface IdentifierSaveDescriptionDto {
  description: string;
  language: string;
  type: string;
}

interface IdentifierFunderSaveDto {
  funder_name: string;
  funder_identifier: string;
  funder_identifier_type: string;
  scheme_uri: string;
  award_number: string;
  award_title: string;
}

interface IdentifierDto {
  id: string;
  database_id: string;
  query_id: string | null;
  table_id: string | null;
  view_id: string | null;
  type: IdentifierTypeDto;
  titles: IdentifierTitleDto[] | [];
  descriptions: IdentifierDescriptionDto[] | [];
  funders: IdentifierFunderDto[] | [];
  query: string | null;
  execution: Date | null;
  doi: string | null;
  publisher: string;
  language: string | null;
  licenses: LicenseDto[] | [];
  creators: CreatorDto[] | [];
  created: Date;
  query_normalized: string | null;
  related_identifiers: RelatedIdentifierDto[] | [];
  query_hash: string | null;
  result_hash: string | null;
  result_number: number | null;
  publication_day: number | null;
  publication_month: number | null;
  publication_year: number;
}

enum IdentifierTypeDto {
  database,
  subset,
  table,
  view
}

enum IdentifierStatusTypeDto {
  draft,
  published
}

interface IdentifierBriefDto {
  id: string;
  database_id: string | null;
  query_id: string | null;
  table_id: string | null;
  view_id: string | null;
  type: IdentifierTypeDto;
  creators: CreatorBriefDto[] | [];
  titles: IdentifierTitleDto[] | [];
  description: IdentifierDescriptionDto[] | [];
  doi: string | null;
  publisher: string;
  publication_year: number;
  status: IdentifierStatusTypeDto;
  owned_by: string;
}

interface IdentifierTitleDto {
  id: string;
  title: string;
  language: string;
  type: string;
}

interface IdentifierDescriptionDto {
  id: string;
  description: string;
  language: string;
  type: string;
}

interface IdentifierFunderDto {
  id: string;
  funder_name: string;
  funder_identifier: string;
  funder_identifier_type: string;
  scheme_uri: string;
  award_number: string;
  award_title: string;
}

enum NameTypeDto {
  Personal,
  Organizational
}

interface CreatorDto {
  id: string;
  firstname: string;
  lastname: string;
  affiliation: string;
  creator_name: string;
  name_type: NameTypeDto | null;
  name_identifier: string | null;
  name_identifier_scheme: string | null;
  name_identifier_scheme_uri: string | null;
  affiliation_identifier: string | null;
  affiliation_identifier_scheme: string | null;
  affiliation_identifier_scheme_uri: string | null;
}

interface CreatorBriefDto {
  id: string;
  affiliation: string;
  creator_name: string;
  name_type: NameTypeDto | null;
  name_identifier: string | null;
  name_identifier_scheme: string | null;
  affiliation_identifier: string | null;
  affiliation_identifier_scheme: string | null;
}

interface RelatedIdentifierDto {
  id: string;
  value: string;
  type: string;
  relation: string;
  created: Date;
  last_modified: Date;
}

interface CreatorSaveDto {
  firstname: string | null;
  lastname: string | null;
  affiliation: string | null;
  creator_name: string;
  name_type: string | null;
  name_identifier: string | null;
  name_identifier_scheme: string | null;
  affiliation_identifier: string | null;
  affiliation_identifier_scheme: string | null;
}

interface RelatedIdentifierSaveDto {
  value: string;
  type: string;
  relation: string;
}

interface ColumnDto {
  id: string;
  name: string;
  alias: string;
  size: number;
  d: number;
  data_length: number;
  max_data_length: number;
  num_rows: number;
  val_min: number;
  val_max: number;
  mean: number;
  median: number;
  std_dev: number;
  concept: ConceptDto;
  unit: UnitDto;
  enums: string[];
  sets: string[];
  database_id: string;
  table_id: string;
  internal_name: string;
  is_primary_key: boolean;
  index_length: number;
  length: number;
  column_type: string;
  is_public: boolean;
  is_null_allowed: boolean;
}

interface ConceptDto {
  id: string;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface UnitDto {
  id: string;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface LicenseDto {
  identifier: string;
  uri: string;
  description: string;
}

interface DatabaseGiveAccessDto {
  type: string;
}

interface DatabaseModifyAccessDto {
  type: string;
}

interface DatabaseModifyVisibilityDto {
  is_public: boolean;
  is_schema_public: boolean;
}

interface DatabaseTransferDto {
  username: string;
}

interface DatabaseModifyImageDto {
  key: string;
}

interface ViewCreateDto {
  name: string;
  query: SubsetDto;
  is_public: boolean;
  is_schema_public: boolean;
}

interface ViewUpdateDto {
  is_public: boolean;
}

interface QueryDto {
  id: string;
  creator: UserDto;
  execution: Date;
  query: string;
  type: string | null;
  identifiers: IdentifierDto[];
  created: Date;
  database_id: string;
  query_normalized: string | null;
  query_hash: string;
  is_persisted: boolean;
  result_hash: string | null;
  /**
   * @deprecated
   */
  result_number: number | null;
  last_modified: Date
}

interface QueryPersistDto {
  persist: boolean;
}

interface TableCsvDto {
  data: Map<string, string>;
}

interface TableCsvDeleteDto {
  keys: Map<string, string>;
}

interface ApiErrorDto {
  status: string;
  message: string;
  code: string;
}

interface KeycloakErrorDto {
  error: string;
  error_description: string;
}

interface SearchResultDto {
  results: any[];
  type: string;
}

interface KeycloakOpenIdTokenDto {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  refresh_expires_in: number;
  id_token: string;
  session_state: string;
  scope: string;
  token_type: string;
  'not-before-policy': number;
}

interface KeycloakErrorDto {
  error: string;
  error_description: string;
}

interface ViewBriefDto {
  id: string;
  database_id: string;
  name: string;
  identifier: any[];
  query: string;
  query_hash: string;
  created: Date;
  creator: UserDto;
  internal_name: string;
  is_public: boolean;
  initial_view: boolean;
  last_modified: Date;
}

interface ViewDto {
  id: string;
  database_id: string;
  name: string;
  identifiers: IdentifierDto[];
  query: string;
  query_hash: string;
  created: Date;
  creator: UserDto;
  internal_name: string;
  is_public: boolean;
  initial_view: boolean;
  last_modified: Date;
}

interface ImageBriefDto {
  id: string;
  name: string;
  version: string;
  jdbc_method: string;
}

interface ImageDto {
  id: string;
  registry: string;
  name: string;
  version: string;
  driver_class: string;
  dialect: string;
  jdbc_method: string;
  default: boolean;
  default_port: number;
  data_types: DataTypeDto[];
  operators: OperatorDto[];
}

interface UserUpdateDto {
  firstname: string;
  lastname: string;
  affiliation: string;
  orcid: string;
}

interface SignupRequestDto {
  username: string;
  password: string;
  email: string;
}

interface UserPasswordDto {
  password: string;
}

interface UserThemeSetDto {
  theme: string;
}

interface ColumnSemanticsUpdateDto {
  concept_uri: string;
  unit_uri: string;
}

interface ImportCsv {
  location: string;
  separator: string;
  quote: string;
  skip_lines: number;
  line_termination: string;
}

interface FilterDto {
  type: string;
  value: string;
  column_id: string;
  operator_id: string;
}

interface OrderDto {
  column_id: string;
  direction: string;
}

interface SubsetDto {
  datasource_id: string;
  datasource_type: string;
  columns: string[];
  filter: FilterDto[] | null;
  order: OrderDto[] | null;
}

interface QueryResultDto {
  id: string | null;
  result: any;
  headers: string[];
}

interface TableHistoryDto {
  timestamp: Date;
  event: string;
  total: number;
}

interface TableCreateDto {
  name: string;
  description: string;
  columns: ColumnCreateDto[];
  constraints: ConstraintsCreateDto;
}

interface ColumnCreateDto {
  name: string;
  type: string;
  size: number | null;
  d: number | null;
  enums: string[];
  sets: string[];
  index_length: number;
  null_allowed: boolean;
}

interface InternalColumnDto {
  name: string;
  type: string;
  size: number;
  d: number;
  enums: string[];
  sets: string[];
  primary_key: boolean;
  index_length: number;
  null_allowed: boolean;
  unique: boolean;
  sets_values: string;
  enums_values: string;
}

interface ConstraintsCreateDto {
  primary_key: string[];
  uniques: string[][];
  checks: string[];
  foreign_keys: ForeignKeyCreateDto[];
}

interface ForeignKeyCreateDto {
  columns: string[];
  referenced_table: string;
  referenced_columns: string[];
  on_update: string;
  on_delete: string;
}

interface OntologyDto {
  id: string;
  uri: string;
  prefix: string;
  sparql: boolean;
  rdf: boolean;
  creator: UserBriefDto;
  created: Date;
  uri_pattern: string;
  sparql_endpoint: string;
  rdf_path: string;
}

interface OntologyModifyDto {
  uri: string;
  prefix: string;
  sparql_endpoint: string;
  rdf_path: string;
}

interface OntologyCreateDto {
  uri: string;
  prefix: string;
  sparql_endpoint: string;
}

interface UnitDto {
  id: string;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface ConceptDto {
  id: string;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface TableColumnEntityDto {
  database_id: string;
  table_id: string;
  column_id: string;
  uri: string;
  label: string;
  description: string;
}

interface ImportDto {
  location: string;
  separator: string;
  quote: string;
  skip_lines: number;
  line_termination: string;
}

interface BannerMessageCreateDto {
  type: string;
  message: string;
  link: string;
  link_text: string;
  display_start: Date;
  display_end: Date;
}

interface BannerMessageUpdateDto {
  type: string;
  message: string;
  link: string;
  link_text: string;
  display_start: Date;
  display_end: Date;
}

interface BannerMessageDto {
  id: string;
  type: string;
  message: string;
  link: string;
  link_text: string;
  display_start: Date;
  display_end: Date;
}

interface FieldsResultDto {
  results: FieldDto[]
}

interface FieldDto {
  attr_friendly_name: string;
  attr_name: string;
  type: string;
}

interface QueryBuildResultDto {
  error: boolean;
  reason: string | null;
  column: string | null;
  raw: string | null;
  formatted: string | null;
}
