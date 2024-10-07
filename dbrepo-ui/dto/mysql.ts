interface DataTypeDto {
  display_name: string;
  value: string;
  size_min: number | null;
  size_max: number | null;
  size_default: number | null;
  size_required: number | null;
  d_min: number | null;
  d_max: number | null;
  d_default: number | null;
  d_required: number | null;
  documentation: string;
  is_quoted: boolean;
  is_buildable: boolean;
}
