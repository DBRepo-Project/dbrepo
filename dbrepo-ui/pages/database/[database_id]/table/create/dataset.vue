<template>
  <div
    v-if="canInsertTableData">
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
    <v-form
      ref="form"
      v-model="valid"
      @submit.prevent="submit">
      <v-card
        variant="flat"
        rounded="0">
        <v-card-text>
          <v-row>
            <v-col
              lg="8">
              <v-alert
                border="start"
                color="info">
                {{ $t('pages.table.subpages.import.dataset.text') }}
                <NuxtLink
                  :href="`/database/${$route.params.database_id}/table/create/schema`">
                  {{ $t('pages.table.subpages.import.schema.text') }}
                </NuxtLink>
              </v-alert>
            </v-col>
          </v-row>
        </v-card-text>
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
                :disabled="step > 4"
                @submit.prevent="submit">
                <v-container>
                  <v-row
                    dense>
                    <v-col
                      lg="4">
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
                    <v-col
                      lg="4">
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
                  <v-row
                    dense>
                    <v-col
                      lg="8">
                      <v-textarea
                        v-model="tableCreate.description"
                        rows="2"
                        clearable
                        counter="180"
                        persistent-counter
                        persistent-hint
                        :variant="inputVariant"
                        :hint="$t('pages.table.subpages.import.description.hint')"
                        :label="$t('pages.table.subpages.import.description.label')"/>
                    </v-col>
                  </v-row>
                  <v-row
                    dense>
                    <v-col
                      lg="4">
                      <v-select
                        v-model="tableCreate.is_public"
                        name="public"
                        :label="$t('pages.database.resource.data.label')"
                        :hint="$t('pages.database.resource.data.hint', { resource: 'table' })"
                        persistent-hint
                        :variant="inputVariant"
                        :items="dataOptions"
                        item-title="title"
                        item-value="value"
                        :rules="[v => v !== null || $t('validation.required')]"
                        required>
                      </v-select>
                    </v-col>
                    <v-col
                      lg="4">
                      <v-select
                        v-model="tableCreate.is_schema_public"
                        name="schema-public"
                        :label="$t('pages.database.resource.schema.label')"
                        :hint="$t('pages.database.resource.schema.hint', { resource: 'table', schema: 'columns' })"
                        persistent-hint
                        :variant="inputVariant"
                        :items="schemaOptions"
                        item-title="title"
                        item-value="value"
                        :rules="[v => v !== null || $t('validation.required')]"
                        required>
                      </v-select>
                    </v-col>
                  </v-row>
                </v-container>
              </v-form>
            </v-stepper-window>
            <TableImport
              :create="true"
              :disabled="!validStep1 || step > 4"
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
                  :disabled="step > 4"
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
                  <v-col
                    lg="8">
                    <v-alert
                      border="start"
                      color="success">
                      {{ $t('pages.table.subpages.create.summary.text') }}
                      <strong>
                        {{ table.internal_name }}
                      </strong>
                    </v-alert>
                  </v-col>
                </v-row>
                <v-row>
                  <v-col>
                    <v-btn
                      class="mb-1 mr-2"
                      color="tertiary"
                      size="small"
                      variant="flat"
                      :loading="loadingImport"
                      :text="$t('navigation.import')"
                      @click="onImport"/>
                    <v-btn
                      class="mb-1"
                      color="secondary"
                      size="small"
                      variant="flat"
                      :loading="loadingContinue"
                      :text="$t('navigation.view')"
                      @click="onContinue"/>
                  </v-col>
                </v-row>
              </v-container>
            </v-stepper-window>
          </v-stepper>
        </v-card-text>
      </v-card>
    </v-form>
    <v-breadcrumbs :items="items" class="pa-0 mt-2"/>
  </div>
</template>

  <script>
import TableSchema from '@/components/table/TableSchema.vue'
import { notEmpty } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    TableSchema
  },
  data() {
    return {
      step: 1,
      valid: false,
      validStep1: false,
      validStep2: false,
      validStep3: false,
      validStep4: false,
      error: false,
      loadingContinue: false,
      loadingImport: false,
      fileModel: null,
      rowCount: null,
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
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
      tables: [],
      tableCreate: {
        name: null,
        is_public: true,
        is_schema_public: true,
        description: '',
        columns: []
      },
      tableImport: {
        location: null,
        quote: '"',
        separator: ',',
        line_termination: null,
        header: true
      },
      loading: false,
      url: null,
      columns: [],
      cacheStore: useCacheStore()
    }
  },
  mounted () {
    if (!this.database) {
      return
    }
    this.tableCreate.is_public = this.database.is_public
    this.tableCreate.is_schema_public = this.database.is_schema_public
  },
  computed: {
    database() {
      return this.cacheStore.getDatabase
    },
    roles () {
      return this.cacheStore.getRoles
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
  methods: {
    notEmpty,
    submit() {
      this.$refs.form.validate()
    },
    createEmptyTableAndImport({success, columns, constraints}) {
      if (!success) {
        return
      }
      const schema = Object.assign({}, this.tableCreate)
      schema.columns = columns
      schema.constraints = constraints
      this.loading = true
      const tableService = useTableService()
      tableService.create(this.$route.params.database_id, schema)
        .then((table) => {
          this.table = table
          const toast = useToastInstance()
          toast.success(this.$t('success.table.created', { table: table.internal_name }))
          this.step = 5
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string' || typeof message !== 'string') {
            /* fallback */
            toast.error(`${this.$t('error.table.create')}: ${message}`)
            return
          }
          toast.error(`${this.$t(code)}: ${message}`)
        })
        .finally(() => {
          this.loading = false
        })
    },
    onAnalyse({columns, filename, line_termination, separator, header, quote}) {
      console.debug('analysed', columns)
      this.tableCreate.columns = columns
      this.tableImport.location = filename
      this.tableImport.line_termination = line_termination
      this.tableImport.separator = separator
      this.tableImport.header = header
      this.tableImport.quote = quote
      if (filename) {
        this.step = 4
      }
    },
    async onImport () {
      this.loadingImport = true
      const cacheStore = useCacheStore()
      cacheStore.reloadDatabase()
      await this.$router.push({ path: `/database/${this.$route.params.database_id}/table/${this.table.id}/import`, query: this.tableImport })
    },
    async onContinue () {
      this.loadingContinue = true
      const cacheStore = useCacheStore()
      cacheStore.reloadDatabase()
      await this.$router.push(`/database/${this.$route.params.database_id}/table/${this.table.id}/data`)
    }
  }
}
</script>
