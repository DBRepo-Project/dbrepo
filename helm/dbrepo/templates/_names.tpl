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
{{ .Values.metadatadb.host }}
{{- end }}

{{/*
Define metadata database connection url
*/}}
{{- define "metadatadb.url" -}}
jdbc:mariadb://{{ .Values.metadatadb.host }}:3306/dbrepo
{{- end }}
