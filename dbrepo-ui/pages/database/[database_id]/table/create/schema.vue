<template>
  <div
    v-if="canCreateTable">
    <v-toolbar
      flat>
      <v-btn
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/table`" />
      <v-toolbar-title
        :text="$t('pages.table.subpages.create.title')" />
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
              :title="$t('pages.table.subpages.create.information.title')"
              :complete="valid"
              :value="1" />
          </v-stepper-header>
          <v-stepper-window
            direction="vertical">
            <v-form
              ref="form"
              v-model="valid"
              :disabled="table"
              @submit.prevent="submit">
              <v-container>
                <v-row dense>
                  <v-col md="4">
                    <v-text-field
                      v-model="tableCreate.name"
                      :rules="[
                        v => notEmpty(v) || $t('validation.required'),
                        v => generatedTableName.length <= 64 || ($t('validation.max-length') + 64),
                      ]"
                      required
                      clearable
                      :variant="inputVariant"
                      :error-messages="!validTableName ? [$t('validation.table.exists')] : []"
                      persistent-hint
                      :hint="$t('pages.table.subpages.import.name.hint')"
                      :label="$t('pages.table.subpages.import.name.label')" />
                  </v-col>
                  <v-col md="4">
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
                      :label="$t('pages.table.subpages.import.generated.label')" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="8">
                    <v-textarea
                      v-model="tableCreate.description"
                      rows="2"
                      :rules="[
                        v => (!v || v.length <= 180) || $t('validation.max-length') + 180
                      ]"
                      clearable
                      counter="180"
                      persistent-counter
                      persistent-hint
                      :variant="inputVariant"
                      :hint="$t('pages.table.subpages.import.description.hint')"
                      :label="$t('pages.table.subpages.import.description.label')" />
                  </v-col>
                </v-row>
                <v-row
                  dense>
                  <v-col
                    md="4">
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
                    md="4">
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
          <v-stepper-header
            step="2">
            <v-stepper-item
              :title="$t('pages.table.subpages.schema.title')"
              :value="2" />
          </v-stepper-header>
          <v-stepper-window
            direction="vertical">
            <v-container>
              <TableSchema
                submit-text="Create"
                :disabled="!valid || table"
                :columns="tableCreate.columns"
                :loading="loading"
                @close="schemaClose" />
            </v-container>
          </v-stepper-window>
          <v-stepper-header
            step="2">
            <v-stepper-item
              :title="$t('pages.table.subpages.schema.summary.title')"
              :value="3" />
          </v-stepper-header>
          <v-stepper-window
            v-if="table"
            direction="vertical">
            <v-container>
              <v-row
                dense>
                <v-col md="8">
                  <v-alert
                    border="start"
                    color="success">
                    {{ $t('pages.table.subpages.schema.summary.text') + ' ' + table.internal_name }}
                  </v-alert>
                </v-col>
              </v-row>
              <v-row>
                <v-col>
                  <v-btn
                    color="tertiary"
                    class="mr-2"
                    variant="flat"
                    size="small"
                    :loading="loadingImport"
                    :text="$t('navigation.import')"
                    @click="onImport" />
                  <v-btn
                    color="secondary"
                    variant="flat"
                    size="small"
                    :loading="loadingContinue"
                    :text="$t('navigation.continue')"
                    @click="onContinue" />
                </v-col>
              </v-row>
            </v-container>
          </v-stepper-window>
        </v-stepper>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
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
  data () {
    return {
      columns: [],
      name: null,
      valid: false,
      description: null,
      loading: false,
      loadingImport: false,
      loadingContinue: false,
      step: 1,
      table: null,
      error: false,
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
      tableCreate: {
        name: null,
        description: null,
        columns: [],
        is_public: true,
        is_schema_public: true,
        constraints: {
          uniques: [],
          foreign_keys: [],
          checks: [],
          primary_key: [],
        }
      },
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
          title: 'Tables',
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: 'Create',
          to: `/database/${this.$route.params.database_id}/table/create`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    generatedTableName () {
      if (!this.tableCreate.name) {
        return null
      }
      const tableService = useTableService()
      return tableService.tableNameToInternalName(this.tableCreate.name)
    },
    database () {
      return this.cacheStore.getDatabase
    },
    roles () {
      return this.cacheStore.getRoles
    },
    canCreateTable () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-table')
    },
    validTableName () {
      if (this.tableCreate.name === null) {
        return true
      }
      if (this.tableCreate.name.length < 3) {
        return true
      }
      return !this.database.tables.map(t => t.internal_name).includes(this.tableCreate.name.toString()
        .normalize('NFKD')
        .toLowerCase()
        .trim()
        .replace(/\s+/g, '-')
        .replace(/[^\w-]+/g, '')
        .replace(/--+/g, '_'))
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
  mounted () {
    if (!this.database) {
      return
    }
    this.tableCreate.is_public = this.database.is_public
    this.tableCreate.is_schema_public = this.database.is_schema_public
  },
  methods: {
    notEmpty,
    submit () {
      this.$refs.form.validate()
    },
    createTable (columns, constraints) {
      this.loading = true
      const tableService = useTableService()
      const payload = Object.assign({}, this.tableCreate)
      payload.columns = columns
      payload.constraints = constraints
      tableService.create(this.$route.params.database_id, payload)
        .then((table) => {
          this.cacheStore.reloadDatabase()
          this.table = table
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(`${code}: ${message}`))
        })
        .finally(() => {
          this.loading = false
        })
    },
    schemaClose ({success, columns, constraints}) {
      console.debug('schema closed', success)
      if (!success) {
        return
      }
      this.createTable(columns, constraints)
    },
    async onImport () {
      this.loadingImport = true
      await this.$router.push(`/database/${this.$route.params.database_id}/table/${this.table.id}/import`)
    },
    async onContinue () {
      this.loadingContinue = true
      await this.$router.push(`/database/${this.$route.params.database_id}/table/${this.table.id}/info`)
    }
  }
}
</script>
