<template>
  <div
    v-if="canUpdateTable">
    <TableToolbar />
    <v-window
      v-model="tab">
      <v-window-item>
        <v-form
          ref="form"
          v-model="valid"
          autocomplete="off"
          @submit.prevent="submit">
          <v-card
            variant="flat"
            rounded="0"
            :title="$t('pages.table.settings.title')"
            :subtitle="$t('pages.table.settings.subtitle')">
            <v-card-text>
              <v-row>
                <v-col
                  lg="8">
                  <v-textarea
                    v-model="modify.description"
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
                    v-model="modify.is_public"
                    :items="dataOptions"
                    persistent-hint
                    :variant="inputVariant"
                    required
                    :rules="[
                      v => v !== null || $t('validation.required')
                    ]"
                    :label="$t('pages.database.resource.data.label')"
                    :hint="$t('pages.database.resource.data.hint', { resource: 'table' })" />
                </v-col>
                <v-col
                  lg="4">
                  <v-select
                    v-model="modify.is_schema_public"
                    :items="schemaOptions"
                    persistent-hint
                    :variant="inputVariant"
                    required
                    :rules="[
                      v => v !== null || $t('validation.required')
                    ]"
                    :label="$t('pages.database.resource.schema.label')"
                    :hint="$t('pages.database.resource.schema.hint', { resource: 'table', schema: 'columns' })" />
                </v-col>
              </v-row>
              <v-row>
                <v-col>
                  <v-btn
                    id="database"
                    variant="flat"
                    size="small"
                    :disabled="!valid || !isChange"
                    :color="buttonColor"
                    :loading="loading"
                    type="submit"
                    :text="$t('navigation.modify')"
                    @click="update" />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-form>
        <v-divider
          v-if="canDropTable" />
        <v-card
          v-if="canDropTable"
          variant="flat"
          rounded="0"
          :loading="loadingDelete"
          :title="$t('pages.table.delete.title')"
          :subtitle="$t('pages.table.delete.subtitle', { table: table.internal_name })">
          <v-card-text>
            <v-row>
              <v-col
                lg="8">
                <v-btn
                  size="small"
                  variant="flat"
                  color="error"
                  @click="askDelete">
                  {{ $t('navigation.delete')}}
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script>
import TableToolbar from '@/components/table/TableToolbar.vue'
import { useCacheStore } from '@/stores/cache.js'
import { max } from '@/utils'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      tab: 0,
      valid: true,
      loading: false,
      loadingDelete: false,
      modify: {
        description: null,
        is_public: null,
        is_schema_public: null
      },
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
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
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`
        },
        {
          title: this.$t('navigation.settings'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/settings`,
          disabled: true
        }
      ],
      headers: [
        { value: 'internal_name', title: this.$t('pages.table.subpages.schema.internal-name.title') },
        { value: 'type', title: this.$t('pages.table.subpages.schema.column-type.title') },
        { value: 'extra', title: this.$t('pages.table.subpages.schema.extra.title') },
        { value: 'column_concept', title: this.$t('pages.table.subpages.schema.concept.title') },
        { value: 'column_unit', title: this.$t('pages.table.subpages.schema.unit.title') },
        { value: 'is_null_allowed', title: this.$t('pages.table.subpages.schema.nullable.title') },
        { value: 'description', title: this.$t('pages.table.subpages.schema.description.title') },
      ],
      dateColumns: [],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    table () {
      return this.cacheStore.getTable
    },
    access () {
      return this.cacheStore.getAccess
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    roles () {
      return this.cacheStore.getRoles
    },
    isChange () {
      if (!this.table) {
        return false
      }
      if (this.table.is_public !== this.modify.is_public || this.table.is_schema_public !== this.modify.is_schema_public) {
        return true
      }
      return this.table.description !== this.modify.description
    },
    canUpdateTable () {
      if (!this.cacheUser || !this.table || !this.access || !this.roles || !this.roles.includes('update-table')) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access) && this.table.owner.username === this.cacheUser.preferred_username
    },
    canDropTable () {
      if (!this.roles || !this.table || !this.cacheUser) {
        return false
      }
      if (this.roles.includes('delete-foreign-table')) {
        return true
      }
      const tableService = useTableService()
      return tableService.isOwner(this.table, this.cacheUser) && this.roles.includes('delete-table') && this.table.identifiers.length === 0
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    buttonColor () {
      return !this.isChange ? null : 'warning'
    }
  },
  mounted() {
    if (!this.table) {
      return
    }
    this.modify.is_public = this.table.is_public
    this.modify.is_schema_public = this.table.is_schema_public
    this.modify.description = this.table.description
  },
  methods: {
    max,
    submit () {
      this.$refs.form.validate()
    },
    extra (column) {
      if (column.type === 'float') {
        return `precision=${column.size}`
      } else if (['decimal', 'double'].includes(column.type)) {
        let extra = ''
        if (column.size !== null) {
          extra += `size=${column.size}`
        }
        if (column.d !== null) {
          if (extra.length > 0) {
            extra += ', '
          }
          extra += `d=${column.d}`
        }
        return extra
      } else if (column.type === 'enum') {
        return `(${column.enums.join(', ')})`
      } else if (column.type === 'set') {
        return `(${column.sets.join(', ')})`
      } else if (['int', 'char', 'varchar', 'binary', 'varbinary', 'tinyint', 'size="small"int', 'mediumint', 'bigint'].includes(column.type)) {
        return column.size !== null ? `size=${column.size}` : ''
      }
      return null
    },
    closed (event) {
      const { success } = event
      console.debug('closed dialog', event)
      if (success) {
        const toast = useToastInstance()
        toast.success(this.$t('success.table.semantics'))
        this.cacheStore.reloadTable()
      }
      this.dialogSemantic = false
    },
    update () {
      this.loading = true
      const tableService = useTableService()
      tableService.update(this.$route.params.database_id, this.$route.params.table_id, this.modify)
        .then(() => {
          this.loading = false
          const toast = useToastInstance()
          toast.success(this.$t('success.table.updated', { table: this.table.internal_name }))
          this.$emit('close', { success: true })
          this.cacheStore.reloadTable()
        })
        .catch(({ code }) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    },
    askDelete () {
      if (!confirm(this.$t('pages.table.delete.subtitle', { table: this.table.internal_name }))) {
        return
      }
      this.loadingDelete = true
      const tableService = useTableService()
      tableService.remove(this.database.id, this.table.id)
        .then(() => {
          this.loadingDelete = false
          console.info('Deleted table with id ', this.table.id)
          this.cacheStore.reloadDatabase()
          const toast = useToastInstance()
          toast.success('Successfully deleted table with id ' + this.table.id)
          this.$router.push(`/database/${this.$route.params.database_id}/table`)
        })
        .catch(({code, message}) => {
          this.loadingDelete = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingDelete = false
        })
    }
  }
}
</script>
