{{/*
Expand the name of the chart.
*/}}
{{- define "kubernetes.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "kubernetes.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "kubernetes.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "kubernetes.labels" -}}
helm.sh/chart: {{ include "kubernetes.chart" . }}
{{ include "kubernetes.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "kubernetes.selectorLabels" -}}
app.kubernetes.io/name: {{ include "kubernetes.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "kubernetes.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "kubernetes.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Service configuration for the sidecar metrics
*/}}
{{- define "dbrepo.logging.sidecarOpensearchService" -}}
[SERVICE]
    Flush                       5
    Daemon                      Off
    Log_Level                   debug
    Parsers_File                /opt/bitnami/fluent-bit/conf/parsers.conf
{{- end -}}

{{/*
Input configuration for the sidecar metrics
*/}}
{{- define "dbrepo.logging.sidecarOpensearchInput" -}}
[INPUT]
    Name                        tail
    Tag                         *
    Path                        /var/log/app/service/*/*.log
    Parser                      docker
    Mem_Buf_Limit               5MB
    Buffer_Chunk_size           32k
    Buffer_Max_size             32k
{{- end -}}

{{/*
Output configuration for the sidecar metrics
*/}}
{{- define "dbrepo.logging.sidecarOpensearchOutput" -}}
[OUTPUT]
    Name                        opensearch
    Match                       *
    Host                        search-db
    Port                        9200
    Index                       logging
    Replace_Dots                On
    Suppress_Type_Name          On
    Trace_Error                 On
{{- end -}}

{{/*
Output configuration for the sidecar metrics
*/}}
{{- define "dbrepo.logging.sidecarMetricsOutput" -}}
[OUTPUT]
    name                        prometheus_exporter
    match                       internal_metrics
    host                        0.0.0.0
    port                        2021
{{- end -}}

{{/*
Broker connections
*/}}
{{- define "dbrepo.broker.connections" -}}
{{- $connections := "" }}
{{- range .Values.ui.public.broker.connections }}
{{- $connections = printf "%s%s%s%s://%s:%s" $connections (ternary "" "," (empty $connections)) (ternary "^" "" .encrypted) .protocol (.host | default $.Values.hostname) .port }}
{{- end }}
{{- printf "%s" $connections }}
{{- end }}