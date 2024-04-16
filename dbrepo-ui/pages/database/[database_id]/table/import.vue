<template>
  <div v-if="canInsertTableData">
    <v-toolbar flat>
      <v-btn
        class="mr-2"
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/table`"/>
      <v-toolbar-title
        :text="$t('pages.table.subpages.import.title')"/>
    </v-toolbar>
    <v-card
      variant="flat"
      rounded="0">
      <v-card-text>
        <v-stepper
          vertical
          variant="flat">
          <v-stepper-header>
            <v-stepper-item
              :title="$t('pages.table.subpages.import.metadata.title')"
              :complete="validStep1"
              :value="1"/>
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
                    <v-text-field
                      v-model="tableCreate.name"
                      :rules="[
                        v => notEmpty(v) || $t('validation.required'),
                        v => generatedTableName.length <= 64 || ($t('validation.max-length') + 64),
                      ]"
                      required
                      clearable
                      :error-messages="!validTableName ? [$t('validation.table.exists')] : []"
                      persistent-hint
                      :variant="inputVariant"
                      :hint="$t('pages.table.subpages.import.name.hint')"
                      :label="$t('pages.table.subpages.import.name.label')"/>
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="8">
                    <v-alert
                      v-if="generatedTableName"
                      class="mt-1"
                      border="start"
                      color="info">
                      {{ $t('pages.table.subpages.import.generated.label') + ' ' + generatedTableName }}
                    </v-alert>
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="8">
                    <v-textarea
                      v-model="tableCreate.description"
                      rows="2"
                      :rules="[
                        v => (!!v || v.length <= 180) || ($t('validation.max-length') + 180),
                      ]"
                      clearable
                      counter="180"
                      persistent-counter
                      persistent-hint
                      :variant="inputVariant"
                      :hint="$t('pages.table.subpages.import.description.hint')"
                      :label="$t('pages.table.subpages.import.description.label')"/>
                  </v-col>
                </v-row>
              </v-container>
            </v-form>
          </v-stepper-window>
          <TableImport
            :step-start="2"
            :create="true"
            :table="table"
            @analyse="onAnalyse"/>
          <v-stepper-header>
            <v-stepper-item
              :title="$t('pages.table.subpages.import.preview.title')"
              :complete="validStep4"
              :value="4"/>
          </v-stepper-header>
          <v-stepper-window
            direction="vertical">
            <v-container
              v-if="step >= 4">
              <TableSchema
                ref="schema"
                :submit-text="$t('navigation.continue')"
                :submit-disabled="!validStep1"
                :columns="tableCreate.columns"
                :loading="loadingCreateAndImport"
                @schema-valid="schemaValidity"
                @close="createEmptyTableAndImport"/>
            </v-container>
          </v-stepper-window>
          <v-stepper-header>
            <v-stepper-item
              :title="$t('pages.table.subpages.import.summary.title')"
              :value="5"/>
          </v-stepper-header>
          <v-stepper-window
            v-if="table"
            direction="vertical">
            <v-container>
              <v-row dense>
                <v-col>
                  <v-alert
                    v-if="rowCount !== null"
                    border="start"
                    color="success">
                    {{ $t('pages.table.subpages.create.summary.prefix') }}
                    <strong v-text="table.internal_name"/>
                    {{ $t('pages.table.subpages.create.summary.middle') }}
                    <strong v-text="rowCount"/>
                    {{ $t('pages.table.subpages.create.summary.suffix') }}
                  </v-alert>
                </v-col>
              </v-row>
              <v-row>
                <v-col>
                  <v-btn
                    class="mb-1"
                    color="secondary"
                    size="small"
                    variant="flat"
                    :text="$t('navigation.data')"
                    :to="`/database/${$route.params.database_id}/table/${table.id}/data`"/>
                </v-col>
              </v-row>
            </v-container>
          </v-stepper-window>
        </v-stepper>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2"/>
  </div>
</template>

<script>
import TableSchema from '@/components/table/TableSchema.vue'
import {notEmpty} from '@/utils'
import {useUserStore} from '@/stores/user'
import {useCacheStore} from '@/stores/cache'

export default {
  components: {
    TableSchema
  },
  data() {
    return {
      step: 1,
      validStep1: false,
      validStep2: false,
      validStep3: false,
      validStep4: false,
      error: false,
      fileModel: null,
      rowCount: null,
      loadingCreateAndImport: false,
      file: {
        filename: null,
        path: null
      },
      table: null,
      separators: [
        {key: ',', value: ','},
        {key: ';', value: ';'},
        {key: '\\t (Tabulator)', value: '\t'}
      ],
      quotes: [
        {key: '" (Double Quotes)', value: '"'},
        {key: '\' (Single Quotes)', value: '\''}
      ],
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: this.$t('navigation.import'),
          to: `/database/${this.$route.params.database_id}/table/import`,
          disabled: true
        }
      ],
      rules: {
        required: value => !!value || 'validation.required'
      },
      dateFormats: [],
      tables: [],
      tableCreate: {
        name: null,
        description: '',
        columns: [],
        constraints: {
          uniques: [],
          checks: [],
          foreign_keys: []
        }
      },
      tableImport: {
        location: null,
        quote: '"',
        false_element: null,
        true_element: null,
        null_element: '',
        separator: ',',
        line_termination: null,
        skip_lines: 1
      },
      loading: false,
      url: null,
      columns: [],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user() {
      return this.userStore.getUser
    },
    roles() {
      return this.userStore.getRoles
    },
    database() {
      return this.cacheStore.getDatabase
    },
    generatedTableName() {
      if (!this.tableCreate.name) {
        return null
      }
      const tableService = useTableService()
      return tableService.tableNameToInternalName(this.tableCreate.name)
    },
    validTableName() {
      if (this.tableCreate.name === null) {
        return true
      }
      if (this.tableCreate.name.length < 3) {
        return true
      }
      if (!this.database || !('tables' in this.database)) {
        return false
      }
      const tableService = useTableService()
      return !this.database
        .tables
        .map(t => t.internal_name)
        .includes(tableService.tableNameToInternalName(this.tableCreate.name))
    },
    canInsertTableData() {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    inputVariant() {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant() {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  mounted() {
    this.loadDateFormats()
  },
  methods: {
    notEmpty,
    onBack() {
      this.step = 1
    },
    submit() {
      this.$refs.form.validate()
    },
    async loadDateFormats() {
      this.loading = true
      const databaseService = useDatabaseService()
      databaseService.findOne(this.$route.params.database_id)
        .then((database) => {
          this.dateFormats = database.container.image.date_formats
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    createEmptyTableAndImport() {
      /* make enum values to array */
      const validColumns = this.tableCreate.columns.map((column) => {
        // validate `id` column: must be a PK
        if (column.name === 'id' && (!column.primary_key)) {
          this.$toast.error(this.$t('error.schema.id'))
          return false
        }
        return true
      })
      // bail out if there is a problem with one of the columns
      if (!validColumns.every(Boolean)) {
        return
      }
      this.tableCreate.columns.forEach(c => {
        if (c.unique) {
          this.tableCreate.constraints.uniques.push([c.name])
        }
        delete c.unique
      })
      const tableService = useTableService()
      this.loadingCreateAndImport = true
      tableService.findAll(this.$route.params.database_id, this.generatedTableName)
        .then((response) => {
          if (response.length !== 0) {
            /* table does exist */
            tableService.remove(this.$route.params.database_id, response[0].id)
              .then(() => {
                this.createTableAndImport(this.tableCreate)
              })
              .catch((error) => {
                this.$toast.error(this.$t('error.import.dataset') + ': ' + error.response.data.message)
                this.loadingCreateAndImport = false
              })
          } else {
            this.createTableAndImport(this.tableCreate)
          }
        })
    },
    createTableAndImport(table) {
      const tableService = useTableService()
      tableService.create(this.$route.params.database_id, table)
        .then((table) => {
          this.table = table
          tableService.importCsv(this.$route.params.database_id, table.id, this.tableImport)
            .then(() => {
              this.$toast.success(this.$t('success.import.dataset'))
              this.cacheStore.reloadDatabase()
              this.loadingCreateAndImport = true
            })
            .catch((error) => {
              console.error('Failed to import csv', error)
              this.$toast.error(this.$t('error.import.dataset') + ': ' + error.response.data.message)
              this.loading = false
              this.$refs.schema.loading = false
              this.loadingCreateAndImport = false
            })
            .finally(() => {
              this.loading = false
              this.loadingCreateAndImport = false
            })
        })
        .catch(() => {
          this.$refs.schema.loading = false
          this.loadingCreateAndImport = false
        })
        .finally(() => {
          this.loading = false
          this.loadingCreateAndImport = false
        })
    },
    schemaValidity(event) {
      const {valid} = event
      this.validStep4 = valid
    },
    onAnalyse(event) {
      const {columns, filename, line_termination} = event
      console.debug('analysed', columns)
      this.tableCreate.columns = columns
      this.tableImport.location = filename
      this.tableImport.line_termination = line_termination
      if (filename) {
        this.step = 4
      }
    }
  }
}
</script>
