interface DatabaseDto {
  id: number;
  name: string;
  creator: UserDto;
  owner: UserDto;
  contact: UserDto;
  created: Date;
  exchange_name: string;
  internal_name: string;
  is_public: boolean;
  description: string | null;
  container: ContainerBriefDto;
  identifiers: IdentifierDto[] | [];
  subsets: IdentifierDto[] | [];
  image: string;
  accesses: DatabaseAccessDto[];
  identifier: IdentifierDto[];
  tables: TableDto[];
  views: ViewDto[];
  exchange_type: string | null;
}

interface DatabaseCreateDto {
  name: string;
  container_id: number;
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
  id: number;
  registry: string;
  name: string;
  version: string;
  dialect: string;
  driver_class: string;
  date_formats: ImageDateDto[];
  jdbc_method: string;
  default_port: number;
}

interface ImageDateDto {
  id: number;
  example: string;
  database_format: string;
  unix_format: string;
  has_time: boolean;
  created_at: Date;
}

interface TableBriefDto {
  id: number;
  name: string;
  description: string;
  owner: UserBriefDto;
  columns: ColumnBriefDto[];
  internal_name: string;
  is_versioned: boolean;
}

interface ColumnBriefDto {
  id: number;
  name: string;
  alias: string;
  database_id: number;
  table_id: number;
  internal_name: string;
  column_type: string;
}

interface TableDto {
  id: number;
  database_id: number;
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
  uid: number;
  table: TableBriefDto;
  columns: ColumnDto[];
}

interface IdentifierSaveDto {
  type: string;
  titles: IdentifierSaveTitleDto[];
  descriptions: IdentifierSaveDescriptionDto[] | [];
  funders: IdentifierFunderSaveDto[] | [];
  licenses: LicenseDto[] | [];
  publisher: string;
  language: string | null;
  creators: CreatorSaveDto[];
  database_id: number | null;
  query_id: number | null;
  view_id: number | null;
  table_id: number | null;
  publication_day: number | null;
  publication_month: number | null;
  publication_year: number;
  related_identifiers: RelatedIdentifierSaveDto[];
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
  id: number;
  type: string;
  titles: IdentifierTitleDto[] | [];
  descriptions: IdentifierDescriptionDto[] | [];
  funders: IdentifierFunderDto[] | [];
  query: string | null;
  execution: Date | null;
  doi: string | null;
  publisher: string | null;
  language: string | null;
  licenses: LicenseDto[] | [];
  creators: CreatorDto[] | [];
  created: Date;
  database_id: number | null;
  query_id: number | null;
  table_id: number | null;
  view_id: number | null;
  query_normalized: string | null;
  related_identifiers: RelatedIdentifierDto[] | [];
  query_hash: string | null;
  result_hash: string | null;
  /**
   * @deprecated
   */
  result_number: number | null;
  publication_day: number | null;
  publication_month: number | null;
  publication_year: number;
  last_modified: Date;
}

interface IdentifierTitleDto {
  id: number;
  title: string;
  language: string;
  type: string;
}

interface IdentifierDescriptionDto {
  id: number;
  description: string;
  language: string;
  type: string;
}

interface IdentifierFunderDto {
  id: number;
  funder_name: string;
  funder_identifier: string;
  funder_identifier_type: string;
  scheme_uri: string;
  award_number: string;
  award_title: string;
}

interface CreatorDto {
  id: number;
  firstname: string;
  lastname: string;
  affiliation: string;
  creator_name: string;
  name_type: string;
  name_identifier: string;
  name_identifier_scheme: string;
  name_identifier_scheme_uri: string;
  affiliation_identifier: string;
  affiliation_identifier_scheme: string;
  affiliation_identifier_scheme_uri: string;
}

interface RelatedIdentifierDto {
  id: number;
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
  id: number;
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
  database_id: number;
  table_id: number;
  internal_name: string;
  date_format: ImageDateDto;
  auto_generated: boolean;
  is_primary_key: boolean;
  index_length: number;
  length: number;
  column_type: string;
  is_public: boolean;
  is_null_allowed: boolean;
}

interface ConceptDto {
  id: number;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface UnitDto {
  id: number;
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
}

interface DatabaseTransferDto {
  username: string;
}

interface DatabaseModifyImageDto {
  key: string;
}

interface ViewCreateDto {
  name: string;
  query: string;
  is_public: boolean;
}

interface QueryDto {
  id: number;
  creator: UserDto;
  execution: Date;
  query: string;
  type: string | null;
  identifiers: IdentifierDto[];
  created: Date;
  database_id: number;
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

interface ExecuteStatementDto {
  statement: string;
  timstamp: Date | null;
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
  id: number;
  database_id: number;
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
  id: number;
  database_id: number;
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
  id: number;
  name: string;
  version: string;
  jdbc_method: string;
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
  false_element: string;
  true_element: string;
  null_element: string;
  line_termination: string;
}

interface QueryResultDto {
  id: number | null;
  result: any;
  headers: any;
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
  size: number;
  d: number;
  dfid: number;
  enums: string[];
  sets: string[];
  primary_key: boolean;
  index_length: number;
  null_allowed: boolean;
}

interface ConstraintsCreateDto {
  uniques: string[];
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
  id: number;
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
  id: number;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface ConceptDto {
  id: number;
  uri: string;
  name: string;
  description: string;
  created: Date;
  columns: ColumnBriefDto[];
}

interface TableColumnEntityDto {
  database_id: number;
  table_id: number;
  column_id: number;
  uri: string;
  label: string;
  description: string;
}

interface ImportDto {
  location: string;
  separator: string;
  quote: string;
  skip_lines: number;
  false_element: string;
  true_element: string;
  null_element: string;
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
  id: number;
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

interface SearchDto {
  field_value_pairs: Map<string, string>;
  search_term: string | null;
  t1: number | null;
  t2: number | null;
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
