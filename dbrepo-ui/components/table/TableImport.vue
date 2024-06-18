<template>
  <div>
    <v-stepper-header>
      <v-stepper-item
        :title="$t('pages.table.subpages.import.dataset.title')"
        :complete="validStep1"
        :value="stepStart"/>
    </v-stepper-header>
    <v-stepper-window
      direction="vertical">
      <v-form
        ref="form"
        v-model="validStep1"
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
              <v-text-field
                v-model.number="tableImport.skip_lines"
                :rules="[
                          v => isNonNegativeInteger(v) || $t('validation.integer')
                        ]"
                type="number"
                required
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.skip.hint')"
                :label="$t('pages.table.subpages.import.skip.label')"/>
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
          <v-row dense>
            <v-col md="8">
              <v-text-field
                v-model="tableImport.null_element"
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.null.hint')"
                :label="$t('pages.table.subpages.import.null.label')"/>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col md="8">
              <v-text-field
                v-model="tableImport.true_element"
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.true.hint')"
                :label="$t('pages.table.subpages.import.true.label')"/>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col md="8">
              <v-text-field
                v-model="tableImport.false_element"
                clearable
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.false.hint')"
                :label="$t('pages.table.subpages.import.false.label')"/>
            </v-col>
          </v-row>
        </v-container>
      </v-form>
    </v-stepper-window>
    <v-stepper-header>
      <v-stepper-item
        :title="$t('pages.table.subpages.import.file.title')"
        :complete="validStep2"
        :value="stepStart+1"/>
    </v-stepper-header>
    <v-stepper-window
      direction="vertical">
      <v-form
        ref="form"
        v-model="validStep2"
        @submit.prevent="submit">
        <v-container>
          <v-row
            v-if="step > 1 && suggestedAnalyseSeparator && providedSeparator !== analysedSeparator"
            dense>
            <v-col>
              <v-alert
                border="start"
                color="warning">
                {{ $t('pages.table.subpages.import.separator.warn.prefix') }}
                <strong v-text="tableImport.separator"/>
                {{ $t('pages.table.subpages.import.separator.warn.middle') }}
                <strong v-text="suggestedAnalyseSeparator"/>
                {{ $t('pages.table.subpages.import.separator.warn.suffix') }}
              </v-alert>
            </v-col>
          </v-row>
          <v-row
            v-if="step > 1 && suggestedAnalyseLineTerminator && providedTerminator !== analysedTerminator"
            dense>
            <v-col>
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
            <v-col>
              <v-alert
                border="start"
                color="warning"
                :text="$t('pages.table.subpages.import.dataset.warn')"/>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="8">
              <v-file-input
                v-model="fileModel"
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
                :label="$t('pages.table.subpages.import.file.label')" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="8">
              <v-btn
                :disabled="!isAnalyseAllowed || !validStep1 || !validStep2"
                :loading="loading"
                :variant="buttonVariant"
                color="secondary"
                size="small"
                :text="$t('pages.table.subpages.import.analyse.text')"
                @click="uploadAndAnalyse"/>
            </v-col>
          </v-row>
        </v-container>
      </v-form>
    </v-stepper-window>
    <v-stepper-header
      v-if="!create">
      <v-stepper-item
        :title="$t('pages.table.subpages.import.summary.title')"
        :value="stepStart+2"/>
    </v-stepper-header>
    <v-stepper-window
      v-if="!create"
      direction="vertical">
      <v-container>
        <v-row
          v-if="rowCount"
          dense>
          <v-col>
            <v-alert
              border="start"
              color="success">
              <span v-text="$t(`pages.table.subpages.import.summary.prefix`)"/>
              <strong>&nbsp;{{ rowCount }}&nbsp;</strong>
              <span v-text="$t('pages.table.subpages.import.summary.suffix')"/>
            </v-alert>
          </v-col>
        </v-row>
        <v-row>
          <v-col>
            <v-btn
              v-if="rowCount !== null"
              color="secondary"
              :disabled="step !== stepStart + 2"
              size="small"
              variant="flat"
              :text="$t('navigation.data')"
              :to="`/database/${$route.params.database_id}/table/${tableId}/data`" />
            <v-btn
              v-else
              color="secondary"
              :disabled="step !== stepStart + 2"
              size="small"
              variant="flat"
              :text="$t('navigation.import')"
              @click="importCsv"/>
          </v-col>
        </v-row>
      </v-container>
    </v-stepper-window>
  </div>
</template>

<script>
import {isNonNegativeInteger} from '@/utils'
import { useCacheStore } from '@/stores/cache'

export default {
  props: {
    tableId: {
      default: () => {
        return null
      }
    },
    stepStart: {
      default: () => {
        return 1
      }
    },
    create: {
      default: () => {
        return false
      }
    }
  },
  data() {
    return {
      step: 1,
      validStep1: false,
      validStep2: false,
      fileModel: null,
      previousFile: null,
      loading: false,
      rowCount: null,
      suggestedAnalyseSeparator: null,
      suggestedAnalyseLineTerminator: null,
      columns: [],
      tableImport: {
        location: null,
        quote: '"',
        false_element: null,
        true_element: null,
        null_element: '',
        separator: ',',
        line_termination: '\\r\\n',
        skip_lines: 1
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
      lineTerminationItems: [
        {name: '\\r\\n (Windows)', value: '\r\n'},
        {name: '\\n (UNIX)', value: '\n'},
        {name: '\\r (pre-OSX)', value: '\r'},
      ],
      cacheStore: useCacheStore()
    }
  },
  watch: {
    stepStart: {
      handler() {
        this.step = this.stepStart
      }
    }
  },
  mounted() {
    this.step = this.stepStart
  },
  computed: {
    table() {
      return this.cacheStore.getTable
    },
    isAnalyseAllowed () {
      if (!this.fileModel || this.fileModel.length === 0) {
        return true
      }
      return this.previousFile !== this.fileModel[0]
    },
    hasCompatibleSchema () {
      if (this.create) {
        return true
      }
      if (!this.columns || !this.table) {
        return false
      }
      const schema = this.table.columns.map(c => c.internal_name)
      let pass = true
      this.columns.forEach(c => {
        if (!schema.includes(c.name)) {
          console.error('Failed to find column with id', c.name, 'in schema')
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
    isNonNegativeInteger,
    submit() {
      this.$refs.form.validate()
    },
    importCsv() {
      this.loading = true
      const tableService = useTableService()
      tableService.importCsv(this.$route.params.database_id, this.tableId, this.tableImport)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.import.dataset'))
          this.cacheStore.reloadDatabase()
          tableService.getCount(this.$route.params.database_id, this.tableId, null)
            .then((rowCount) => {
              this.rowCount = rowCount
            })
          this.step = this.stepStart + 2
          this.loading = false
        })
        .catch(() => {
          const toast = useToastInstance()
          toast.error(this.$t('error.import.dataset'))
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    uploadAndAnalyse() {
      this.loading = true
      this.previousFile = this.fileModel[0]
      const uploadService = useUploadService()
      return uploadService.create(this.previousFile)
        .then((s3key) => {
          const toast = useToastInstance()
          toast.success(this.$t('success.upload.dataset'))
          this.analyse(s3key)
        })
        .catch(() => {
          const toast = useToastInstance()
          toast.error(this.$t('error.upload.dataset'))
          this.loading = false
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
          const queryService = useQueryService()
          const dataTypes = queryService.mySql8DataTypes()
          this.columns = Object.entries(columns)
            .map(([key, val]) => {
              return {
                name: key,
                type: val,
                null_allowed: true,
                primary_key: false,
                size: dataTypes.filter(d => d.value === val).length > 0 ? dataTypes.filter(d => d.value === val)[0].defaultSize : null,
                d: dataTypes.filter(d => d.value === val).length > 0 ? dataTypes.filter(d => d.value === val)[0].defaultD : null,
                enums: [],
                sets: []
              }
            })
          this.suggestedAnalyseSeparator = separator
          this.suggestedAnalyseLineTerminator = line_termination
          this.tableImport.location = filename
          this.step = this.stepStart + 2
          const toast = useToastInstance()
          toast.success(this.$t('success.analyse.dataset'))
          this.$emit('analyse', {columns: this.columns, filename, line_termination})
          this.loading = false
        })
        .catch(({code}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
    }
  }
}
</script>
