{{/*
Allow the release namespace to be overridden for multi-namespace deployments in combined charts.
*/}}
{{- define "common.names.namespace" -}}
{{- default .Release.Namespace .Values.namespaceOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Define metadata database connection hostname
*/}}
{{- define "metadatadb.endpoint" -}}
{{- if .Values.metadatadb.pooler.enabled }}
metadata-db-pooler-rw
{{- else }}
{{ .Values.metadatadb.host }}
{{- end }}
{{- end }}

{{/*
Define metadata database connection url
*/}}
{{- define "metadatadb.url" -}}
{{- if .Values.metadatadb.pooler.enabled }}
jdbc:postgresql://metadata-db-pooler-rw:5432/dbrepo
{{- else }}
jdbc:postgresql://{{ .Values.metadatadb.host }}:5432/dbrepo
{{- end }}
{{- end }}

{{/*
Define data database connection hostname
*/}}
{{- define "datadb.endpoint" -}}
{{- if .Values.datadb.pooler.enabled }}
data-db-pooler-rw
{{- else }}
{{ .Values.datadb.host }}
{{- end }}
{{- end }}

{{/*
Define data database connection url
*/}}
{{- define "datadb.url" -}}
{{- if .Values.datadb.pooler.enabled }}
jdbc:postgresql://data-db-pooler-rw:5432
{{- else }}
jdbc:postgresql://{{ .Values.datadb.host }}:5432
{{- end }}
{{- end }}