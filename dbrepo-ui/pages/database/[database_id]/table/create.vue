<template>
  <div v-if="canCreateTable">
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
                  <v-col md="8">
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
                      :label="$t('pages.table.subpages.import.generated.label')" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="8">
                    <v-textarea
                      v-model="tableCreate.description"
                      rows="2"
                      variant="underlined"
                      :rules="[
                        v => (!!v || v.length <= 180) || ($t('validation.max-length') + 180),
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
                :disabled="!tableCreate.name || table"
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
            direction="vertical">
            <v-container v-if="table">
              <v-row
                dense>
                <v-col>
                  <v-alert
                    border="start"
                    color="success"
                    v-text="$t('pages.table.subpages.schema.summary.text') + ' ' + table.internal_name" />
                </v-col>
              </v-row>
              <v-row>
                <v-col>
                  <v-btn
                    color="secondary"
                    variant="flat"
                    size="small"
                    :text="$t('navigation.continue')"
                    :to="`/database/${this.$route.params.database_id}/table/${table.id}/info`" />
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
import TableSchema from '@/components/table/TableSchema'
import { notEmpty } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
      step: 1,
      table: null,
      error: false,
      tableCreate: {
        name: null,
        description: null,
        columns: []
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
      userStore: useUserStore(),
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
    roles () {
      return this.userStore.getRoles
    },
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
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
  },
  methods: {
    notEmpty,
    submit () {
      this.$refs.form.validate()
    },
    createTable () {
      this.loading = true
      const tableService = useTableService()
      tableService.create(this.$route.params.database_id, this.tableCreate)
        .then((table) => {
          this.cacheStore.reloadDatabase()
          this.table = table
        })
        .catch((error) => {
          this.$toast.error(this.$t('error.table.create'))
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    schemaClose (event) {
      console.debug('schema closed', event)
      if (!event.success) {
        return
      }
      this.createTable()
    }
  }
}
</script>
