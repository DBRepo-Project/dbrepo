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
                    <v-text-field
                      v-model="generatedTableName"
                      :rules="[
                        v => notEmpty(v) || $t('validation.required'),
                        v => generatedTableName.length <= 64 || ($t('validation.max-length') + 64),
                      ]"
                      disabled
                      clearable
                      counter="64"
                      persistent-counter
                      persistent-hint
                      :variant="inputVariant"
                      :hint="$t('pages.table.subpages.import.generated.hint')"
                      :label="$t('pages.table.subpages.import.generated.label')"/>
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
                :back="false"
                :loading="loading"
                :submit-text="$t('navigation.continue')"
                :columns="tableCreate.columns"
                @close="createEmptyTableAndImport"/>
            </v-container>
          </v-stepper-window>
          <v-stepper-header>
            <v-stepper-item
              :title="$t('pages.table.subpages.import.summary.title')"
              :value="5"/>
          </v-stepper-header>
          <v-stepper-window
            v-if="step >= 5"
            direction="vertical">
            <v-container>
              <v-row dense>
                <v-col>
                  <v-alert
                    border="start"
                    color="success">
                    {{ $t('pages.table.subpages.create.summary.prefix') }}
                    <strong v-text="table.internal_name"/>
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
                    :loading="loadingContinue"
                    :text="$t('navigation.data')"
                    @click="onContinue"/>
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
      loadingContinue: false,
      fileModel: null,
      rowCount: null,
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
        columns: []
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
    createEmptyTableAndImport({success, columns, constraints}) {
      if (!success) {
        return
      }
      const payload = Object.assign({}, this.tableCreate)
      payload.columns = columns
      payload.constraints = constraints
      this.createTable(payload)
        .then(table => this.import(table))
    },
    createTable(payload) {
      this.loading = true
      const tableService = useTableService()
      return new Promise((resolve, reject) => {
        if (this.table) {
          resolve(this.table)
          return
        }
        tableService.create(this.$route.params.database_id, payload)
        .then((table) => {
          this.table = table
          resolve(table)
        })
        .catch((error) => {
          console.error('Failed to create table', error)
          this.$toast.error(this.$t(error.code))
          this.loading = false
          reject(error)
        })
        .finally(() => {
          this.loading = false
        })
      })
    },
    import(table) {
      this.loading = true
      const tableService = useTableService()
      tableService.importCsv(this.$route.params.database_id, table.id, this.tableImport)
        .then(() => {
          this.step = 5
          this.$toast.success(this.$t('success.import.dataset'))
          this.cacheStore.reloadDatabase()
        })
        .catch((error) => {
          console.error('Failed to import csv', error)
          this.$toast.error(this.$t(error.code))
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    schemaValidity(event) {
      const {valid} = event
      this.validStep4 = valid
    },
    onAnalyse({columns, filename, line_termination}) {
      console.debug('analysed', columns)
      this.tableCreate.columns = columns
      this.tableImport.location = filename
      this.tableImport.line_termination = line_termination
      if (filename) {
        this.step = 4
      }
    },
    async onContinue () {
      this.loadingContinue = true
      await this.$router.push(`/database/${this.$route.params.database_id}/table/${this.table.id}/data`)
    }
  }
}
</script>
