<template>
  <div>
    <v-stepper-header>
      <v-stepper-item
        :title="$t('pages.table.subpages.import.schema.title')"
        :complete="validStep1"
        :value="1"/>
    </v-stepper-header>
    <v-stepper-window
      direction="vertical">
      <v-form
        ref="form"
        v-model="validStep1"
        :disabled="disabled"
        @submit.prevent="submit">
        <v-container>
          <v-row dense>
            <v-col md="8">
              <v-select
                v-model="tableImport.separator"
                :items="separators"
                item-title="key"
                item-value="value"
                required
                clearable
                persistent-hint
                :base-color="suggestedAnalyseSeparator && providedSeparator !== analysedSeparator ? 'warning' : ''"
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.separator.hint')"
                :label="$t('pages.table.subpages.import.separator.label')"/>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col md="8">
              <v-select
                v-model="tableImport.header"
                :items="headers"
                item-title="key"
                item-value="value"
                required
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.skip.hint')"
                :label="$t('pages.table.subpages.import.skip.label')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col md="8">
              <v-select
                v-model="tableImport.quote"
                :items="quotes"
                item-title="key"
                item-value="value"
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.quote.hint')"
                :label="$t('pages.table.subpages.import.quote.label')"/>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col md="8">
              <v-select
                v-model="tableImport.line_termination"
                :items="lineTerminationItems"
                item-title="name"
                item-value="value"
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.terminator.hint')"
                :label="$t('pages.table.subpages.import.terminator.label')">
                <template
                    v-if="suggestedAnalyseLineTerminator && providedTerminator !== analysedTerminator"
                    v-slot:prepend>
                  <v-icon
                    color="warning">
                    mdi-alert-outline
                  </v-icon>
                </template>
              </v-select>
            </v-col>
          </v-row>
        </v-container>
      </v-form>
    </v-stepper-window>
    <v-stepper-header>
      <v-stepper-item
        :title="$t('pages.table.subpages.import.file.title')"
        :complete="validStep2"
        :value="2" />
    </v-stepper-header>
    <v-stepper-window
      direction="vertical">
      <v-container>
        <v-row
          v-if="$route.query.location"
          dense>
          <v-col>
            <p>
              {{ $t('pages.table.subpages.import.storage.text') }}
            </p>
            <v-chip
              prepend-icon="mdi-cloud-upload"
              label>
              {{ $route.query.location }}
            </v-chip>
          </v-col>
        </v-row>
        <v-form
          ref="form"
          v-model="validStep2"
          :disabled="disabled"
          @submit.prevent="submit">
            <v-row
              v-if="step > 1 && suggestedAnalyseSeparator && providedSeparator !== analysedSeparator"
              dense>
              <v-col
                md="8">
                <v-alert
                  border="start"
                  color="warning">
                  {{ $t('pages.table.subpages.import.separator.warn.prefix') }}
                  <strong>
                    {{ tableImport.separator }}
                  </strong>
                  {{ $t('pages.table.subpages.import.separator.warn.middle') }}
                  <strong>
                    {{ suggestedAnalyseSeparator }}
                  </strong>
                  {{ $t('pages.table.subpages.import.separator.warn.suffix') }}
                </v-alert>
              </v-col>
            </v-row>
            <v-row
              v-if="step > 1 && suggestedAnalyseLineTerminator && providedTerminator !== analysedTerminator"
              dense>
              <v-col
                md="8">
                <v-alert
                  border="start"
                  color="warning">
                  {{ $t('pages.table.subpages.import.terminator.warn.prefix') }}
                  <strong>{{ JSON.stringify(tableImport.line_termination).replaceAll('"', '') }}</strong>
                  {{ $t('pages.table.subpages.import.terminator.warn.middle') }}
                  <strong>{{ JSON.stringify(suggestedAnalyseLineTerminator).replaceAll('"', '') }}</strong>
                  {{ $t('pages.table.subpages.import.terminator.warn.suffix') }}
                </v-alert>
              </v-col>
            </v-row>
            <v-row
              v-if="!hasCompatibleSchema"
              dense>
              <v-col
                md="8">
                <v-alert
                  border="start"
                  color="warning"
                  :text="$t('pages.table.subpages.import.dataset.warn')"/>
              </v-col>
            </v-row>
            <v-row
              v-if="!$route.query.location"
              dense>
              <v-col cols="8">
                <v-file-input
                  v-model="file"
                  accept=".csv,.tsv"
                  :show-size="1000"
                  counter
                  required
                  :rules="[
                    v => notFile(v) || $t('validation.required'),
                  ]"
                  :prepend-icon="validStep1 ? 'mdi-database-check-outline' : 'mdi-database-arrow-up-outline'"
                  persistent-hint
                  :variant="inputVariant"
                  :hint="$t('pages.table.subpages.import.file.hint')"
                  :label="$t('pages.table.subpages.import.file.label')">
                  <template
                    v-if="uploadProgress"
                    v-slot:append>
                    <span>{{ uploadProgress }}%</span>
                  </template>
                </v-file-input>
              </v-col>
            </v-row>
            <v-row
              dense>
              <v-col
                cols="8">
                <v-btn
                  v-if="create && !$route.query.location"
                  :disabled="!isAnalyseAllowed || !validStep1 || !validStep2 || disabled"
                  :loading="loading"
                  :variant="buttonVariant"
                  color="secondary"
                  size="small"
                  :text="$t('pages.table.subpages.import.analyse.text')"
                  @click="uploadAndAnalyse"/>
                <v-btn
                  v-if="!create && !$route.query.location"
                  :disabled="!isAnalyseAllowed || !validStep1 || !validStep2 || disabled"
                  :loading="loading || loadingImport"
                  :variant="buttonVariant"
                  color="secondary"
                  size="small"
                  :text="$t('pages.table.subpages.import.upload.text')"
                  @click="uploadAndImport"/>
                <v-btn
                  v-if="!create && $route.query.location"
                  :disabled="step > 2 || disabled"
                  :loading="loading || loadingImport"
                  :variant="buttonVariant"
                  color="secondary"
                  size="small"
                  class="mt-2"
                  :text="$t('pages.table.subpages.import.text')"
                  @click="importCsv"/>
              </v-col>
            </v-row>
          </v-form>
        </v-container>
    </v-stepper-window>
    <v-stepper-header
      v-if="!create">
      <v-stepper-item
        :title="$t('pages.table.subpages.import.summary.title')"
        :value="3"/>
    </v-stepper-header>
    <v-stepper-window
      v-if="!create && step === 3"
      direction="vertical">
      <v-container>
        <v-row
          dense>
          <v-col
            md="8">
            <v-alert
              border="start"
              color="success">
              <span>
                {{ $t(`pages.table.subpages.import.summary.text`)}}
              </span>
            </v-alert>
          </v-col>
        </v-row>
        <v-row>
          <v-col>
            <v-btn
              v-if="step === 3"
              color="secondary"
              :disabled="step !== 3 || disabled"
              size="small"
              variant="flat"
              :text="$t('navigation.data')"
              :to="`/database/${$route.params.database_id}/table/${tableId}/data`" />
          </v-col>
        </v-row>
      </v-container>
    </v-stepper-window>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'

export default {
  props: {
    tableId: {
      default: () => {
        return null
      }
    },
    create: {
      default: () => {
        return false
      }
    },
    disabled: {
      default: () => {
        return false
      }
    }
  },
  data() {
    return {
      step: 2,
      validStep1: false,
      validStep2: false,
      validStep3: false,
      file: null,
      loading: false,
      loadingImport: false,
      rowCount: null,
      suggestedAnalyseSeparator: null,
      suggestedAnalyseLineTerminator: null,
      columns: [],
      tableImport: {
        location: null,
        quote: '"',
        separator: ',',
        line_termination: '\\n',
        header: true
      },
      separators: [
        {key: ',', value: ','},
        {key: ';', value: ';'},
        {key: '[Tab]', value: '\t'}
      ],
      quotes: [
        {key: 'Double "', value: '"'},
        {key: 'Single \'', value: '\''}
      ],
      headers: [
        {key: 'First line is header', value: true},
        {key: 'Data only', value: false}
      ],
      lineTerminationItems: [
        {name: '\\r\\n (Windows)', value: '\r\n'},
        {name: '\\n (UNIX)', value: '\n'},
        {name: '\\r (pre-OSX)', value: '\r'},
      ],
      cacheStore: useCacheStore()
    }
  },
  mounted() {
    this.cacheStore.setUploadProgress(null)
    this.setQueryParamSafely('location')
    this.setQueryParamSafely('quote')
    this.setQueryParamSafely('separator')
    this.setQueryParamSafely('line_termination')
    this.setQueryParamSafely('header', true)
    if (this.$route.query.location) {
      this.step = 2
      this.validStep2 = true
    }
  },
  computed: {
    table() {
      return this.cacheStore.getTable
    },
    uploadProgress () {
      return this.cacheStore.getUploadProgress
    },
    isAnalyseAllowed () {
      if (!this.file || this.file.length === 0) {
        return false
      }
      return true
    },
    hasCompatibleSchema () {
      if (this.create) {
        return true
      }
      if (!this.columns || !this.table) {
        return false
      }
      const schema = this.table.columns.map(c => c.name)
      let pass = true
      this.columns.forEach(c => {
        if (!schema.includes(c.name)) {
          console.error('Failed to find column', c.name, 'in schema')
          pass = false
        }
      })
      return pass
    },
    providedTerminator() {
      if (this.tableImport.line_termination === null) {
        return null
      }
      return this.tableImport.line_termination.replace(/(\n)/g, function ($0) {
        return $0 === ' ' ? ' ' : '\\n'
      })
    },
    analysedTerminator() {
      if (this.suggestedAnalyseLineTerminator === null) {
        return null
      }
      return this.suggestedAnalyseLineTerminator.replace(/(\n)/g, function ($0) {
        return $0 === ' ' ? ' ' : '\\n'
      })
    },
    providedSeparator() {
      if (this.tableImport.separator === null) {
        return null
      }
      return this.tableImport.separator.replace(/(\n)/g, function ($0) {
        return $0 === ' ' ? ' ' : '\\n'
      })
    },
    analysedSeparator() {
      if (this.suggestedAnalyseSeparator === null) {
        return null
      }
      return this.suggestedAnalyseSeparator.replace(/(\n)/g, function ($0) {
        return $0 === ' ' ? ' ' : '\\n'
      })
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate()
    },
    setQueryParamSafely(name, parse_bool = false) {
      if (this.$route.query[name]) {
        var value = this.$route.query[name]
        if (parse_bool) {
          value = this.$route.query[name] === 'true'
        }
        this.tableImport[name] = value
      }
    },
    importCsv() {
      this.loadingImport = true
      const tableService = useTableService()
      tableService.importCsv(this.$route.params.database_id, this.tableId, this.tableImport)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.import.dataset'))
          this.cacheStore.reloadDatabase()
          this.step = 3
          this.validStep3 = true
          this.loadingImport = false
        })
        .catch(({code, message}) => {
          const toast = useToastInstance()
          console.error(code, message)
          toast.error(`${this.$t(code)}: ${message}`)
          this.loadingImport = false
        })
        .finally(() => {
          this.loadingImport = false
        })
    },
    uploadAndAnalyse() {
      this.upload()
        .then((s3key) => {
          this.analyse(s3key)
        })
    },
    uploadAndImport() {
      this.upload()
        .then((s3key) => {
          this.tableImport.location = s3key
          this.importCsv()
        })
    },
    upload() {
      this.loading = true
      console.debug('upload file', this.file)
      const uploadService = useUploadService()
      return new Promise((resolve, reject) => {
        return uploadService.create(this.file)
          .then((s3key) => {
            const toast = useToastInstance()
            toast.success(this.$t('success.upload.dataset'))
            this.loading = false
            resolve(s3key)
          })
          .catch((error) => {
            console.error('Failed to upload dataset', error)
            const toast = useToastInstance()
            toast.error(this.$t('error.upload.dataset'))
            this.loading = false
            reject(error)
          })
      })
    },
    analyse(filename) {
      const analyseService = useAnalyseService()
      const payload = { filename }
      if (this.tableImport.separator) {
        payload.separator = this.tableImport.separator
      }
      this.loading = true
      analyseService.suggest(payload)
        .then((analysis) => {
          const {columns, separator, line_termination} = analysis
          this.columns = Object.entries(columns)
            .map(([name, analyse]) => {
              return {
                name: name.trim(),
                type: analyse.type,
                null_allowed: analyse.null_allowed,
                primary_key: false,
                size: analyse.size,
                d: analyse.d,
                enums: analyse.enums,
                sets: analyse.sets
              }
            })
          this.suggestedAnalyseSeparator = separator
          this.suggestedAnalyseLineTerminator = line_termination
          this.tableImport.location = filename
          this.step = 3
          this.cacheStore.setUploadProgress(null)
          const toast = useToastInstance()
          toast.success(this.$t('success.analyse.dataset'))
          this.$emit('analyse', {
            columns: this.columns,
            filename,
            line_termination: line_termination === '\\n' ? '\n' : JSON.stringify(line_termination).replaceAll('"', ''),
            separator: this.tableImport.separator,
            header: this.tableImport.header,
            quote: this.tableImport.quote,
          })
          this.loading = false
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            /* fallback default error message */
            toast.error(this.$t('error.analyse.invalid'))
            return
          }
          toast.error(`${this.$t(code)}: ${message}`)
        })
    }
  }
}
</script>
