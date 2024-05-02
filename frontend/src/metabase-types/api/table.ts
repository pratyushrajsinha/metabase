import type { Database, DatabaseId, InitialSyncStatus } from "./database";
import type { Field, FieldDimensionOption, FieldId } from "./field";
import type { Segment } from "./segment";

export type ConcreteTableId = number;
export type VirtualTableId = string; // e.g. "card__17" where 17 is a card id
export type TableId = ConcreteTableId | VirtualTableId;
export type SchemaId = string;

export type TableVisibilityType =
  | null
  | "details-only"
  | "hidden"
  | "normal"
  | "retired"
  | "sensitive"
  | "technical"
  | "cruft";

export type TableFieldOrder = "database" | "alphabetical" | "custom" | "smart";

export interface Table {
  id: TableId;

  name: string;
  display_name: string;
  description: string | null;

  db_id: DatabaseId;
  db?: Database;

  schema: string;

  fks?: ForeignKey[];
  fields?: Field[];
  segments?: Segment[];
  dimension_options?: Record<string, FieldDimensionOption>;
  field_order: TableFieldOrder;

  active: boolean;
  visibility_type: TableVisibilityType;
  initial_sync_status: InitialSyncStatus;
  caveats?: string;
  points_of_interest?: string;
}

export type SchemaName = string;

export interface Schema {
  id: SchemaId;
  name: SchemaName;
}

export interface SchemaListQuery {
  dbId: DatabaseId;
  include_hidden?: boolean;
  include_editable_data_model?: boolean;
}

export interface TableMetadataQuery {
  include_sensitive_fields?: boolean;
  include_hidden_fields?: boolean;
  include_editable_data_model?: boolean;
}

export interface TableListQuery {
  dbId?: DatabaseId;
  schemaName?: string;
  include_hidden?: boolean;
  include_editable_data_model?: boolean;
  remove_inactive?: boolean;
}

export interface ForeignKey {
  origin?: Field;
  origin_id: FieldId;
  destination?: Field;
  destination_id: FieldId;
  relationship: "Mt1";
}

export interface GetTableRequest {
  id: TableId;
  include_editable_data_model?: boolean;
}

export interface GetTableMetadataRequest {
  id: TableId;
  include_sensitive_fields?: boolean;
  include_hidden_fields?: boolean;
  include_editable_data_model?: boolean;
}

export interface UpdateTableRequest {
  id: TableId;
  display_name?: string;
  visibility_type?: TableVisibilityType;
  description?: string;
  caveats?: string;
  points_of_interest?: string;
  show_in_getting_started?: boolean;
  field_order?: TableFieldOrder;
}

export interface UpdateTableListRequest {
  ids: TableId[];
  display_name?: string;
  visibility_type?: TableVisibilityType;
  description?: string;
  caveats?: string;
  points_of_interest?: string;
  show_in_getting_started?: boolean;
}

export interface UpdateTableFieldsOrderRequest {
  id: TableId;
  field_order: FieldId[];
}
