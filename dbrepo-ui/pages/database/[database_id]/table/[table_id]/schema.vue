<template>
  <div
    v-if="canViewSchema">
    <TableToolbar
      :selection="selection" />
    <v-toolbar
      color="secondary"
      :title="$t('pages.table.subpages.schema.title')"
      variant="flat" />
    <v-card
      variant="flat"
      rounded="0"
      tile>
      <v-data-table
        v-if="table"
        class="full-width"
        disable-sort
        hide-default-footer
        :items-per-page="-1"
        :headers="headers"
        :items="table.columns">
        <template v-slot:item.is_null_allowed="{ item }">
          <v-switch
            v-if="canModifyConstraints"
            color="secondary"
            density="compact"
            hide-details
            :model-value="item.is_null_allowed"
            :disabled="loadingConstraint || isPrimaryKeyColumn(item)"
            @update:modelValue="toggleNull(item, $event)" />
          <span
            v-else-if="item.is_null_allowed">
            {{ $t('pages.table.subpages.schema.bullet') }}
          </span>
          <span v-else>{{ item.is_null_allowed }}</span>
        </template>
        <template v-slot:item.extra="{ item }">
          <pre>{{ extra(item) }}</pre>
        </template>
        <template v-slot:item.column_concept="{ item }">
          <a
            v-if="conceptUri(item) && !canAssignSemanticInformation"
            :href="conceptUri(item)"
            v-bind="props">
            {{ conceptUri(item) }}
          </a>
          <v-tooltip
            v-if="conceptUri(item) && !canAssignSemanticInformation && tooltip(item, 'concept')"
            :text="tooltip(item, 'concept')">
            <template
              v-slot:activator="{ props }">
              <v-icon
                class="ml-1"
                v-bind="props">
                mdi-information-slab-circle-outline
              </v-icon>
            </template>
          </v-tooltip>
          <v-text-field
            v-if="canAssignSemanticInformation"
            :model-value="conceptUri(item)"
            readonly
            variant="plain">
            <template
              v-slot:append>
              <v-btn
                variant="flat"
                size="xs"
                @click="pick(item)">
                <v-icon>mdi-pencil</v-icon>
              </v-btn>
            </template>
          </v-text-field>
        </template>
        <template v-slot:item.column_unit="{ item }">
          <a
            v-if="unitUri(item) && !canAssignSemanticInformation"
            :href="unitUri(item)"
            v-bind="props">
            {{ unitUri(item) }}
          </a>
          <v-tooltip
            v-if="unitUri(item) && !canAssignSemanticInformation && tooltip(item, 'unit')"
            :text="tooltip(item, 'unit')">
            <template
              v-slot:activator="{ props }">
              <v-icon
                class="ml-1"
                v-bind="props">
                mdi-information-slab-circle-outline
              </v-icon>
            </template>
          </v-tooltip>
          <v-text-field
            v-if="canAssignSemanticInformation"
            :model-value="unitUri(item)"
            readonly
            variant="plain">
            <template
              v-slot:append>
              <v-btn
                variant="flat"
                size="xs"
                @click="pick(item)">
                <v-icon>mdi-pencil</v-icon>
              </v-btn>
            </template>
          </v-text-field>
        </template>
        <template v-slot:item.description="{ item }">
          <span
            v-if="item.description && !canAssignSemanticInformation"
            v-text="item.description" />
          <v-text-field
            v-if="canAssignSemanticInformation"
            :model-value="item.description"
            readonly
            variant="plain">
            <template
              v-slot:append>
              <v-btn
                variant="flat"
                size="xs"
                @click="pick(item)">
                <v-icon>mdi-pencil</v-icon>
              </v-btn>
            </template>
          </v-text-field>
        </template>
      </v-data-table>
    </v-card>
    <v-card
      v-if="table && hasStructure"
      variant="flat"
      rounded="0"
      tile
      :title="$t('pages.table.subpages.schema.subtitle')">
      <v-card-text>
        <v-container>
          <v-row
            v-if="canModifyConstraints"
            class="mb-2"
            dense>
            <v-col cols="auto">
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :loading="loadingConstraint"
                :text="$t('pages.table.subpages.schema.constraints.add_unique.text')"
                @click="openUniqueDialog" />
            </v-col>
            <v-col cols="auto">
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :loading="loadingConstraint"
                :text="$t('pages.table.subpages.schema.constraints.add_foreign_key.text')"
                @click="openForeignKeyDialog" />
            </v-col>
            <v-col cols="auto">
              <v-btn
                size="small"
                color="tertiary"
                :variant="buttonVariant"
                :loading="loadingConstraint"
                :text="$t('pages.table.subpages.schema.constraints.add_check.text')"
                @click="openCheckDialog" />
            </v-col>
          </v-row>
          <ul>
            <li v-if="table.constraints.primary_key.length > 0">
              <strong
                class="text-uppercase"
                v-text="$t('pages.table.subpages.schema.primary_key.label')" />
              (<i>{{ primaryKeysColumns }}</i>)
            </li>
            <li v-for="(foreignKey, i) in table.constraints.foreign_keys" :key="`fk-${i}`">
              <strong
                class="text-uppercase"
                v-text="$t('pages.table.subpages.schema.foreign_key.label')" />
              <span class="ml-1">{{ foreignKey.name }}</span> (<i>{{ foreignKeyColumns(foreignKey) }}</i>) 
              <strong>REFERENCES</strong> <a :href="`/database/${database.id}/table/${foreignKey.referenced_table.id}/schema`">{{ foreignKeyReferencedTable(foreignKey) }}</a> (<i>{{ foreignKeyReferencedColumns(foreignKey) }}</i>) 
              <strong>ON DELETE</strong> <span v-if="!!foreignKey.on_delete">{{ foreignKey.on_delete.replaceAll("_"," ").toUpperCase() }}</span> <span v-else>NO ACTION</span> 
              <strong class="ml-1">ON UPDATE</strong> <span v-if="!!foreignKey.on_update">{{ foreignKey.on_update.replaceAll("_"," ").toUpperCase() }}</span> <span v-else>NO ACTION</span> 
              <v-btn
                v-if="canModifyConstraints"
                class="ml-1"
                density="compact"
                size="x-small"
                variant="text"
                color="error"
                icon="mdi-delete"
                :loading="loadingConstraint"
                :aria-label="$t('pages.table.subpages.schema.constraints.delete.text')"
                :title="$t('pages.table.subpages.schema.constraints.delete.text')"
                @click="deleteConstraint({ type: 'foreign_key', name: foreignKey.name })" />
            </li>
            <li v-for="(uniqueConstraint, i) in table.constraints.uniques" :key="`uk-${i}`">
              <strong
                class="text-uppercase"
                v-text="$t('pages.table.subpages.schema.unique.label')" />
              (<i>{{ uniqueColumns(uniqueConstraint) }}</i>)
              <v-btn
                v-if="canModifyConstraints"
                class="ml-1"
                density="compact"
                size="x-small"
                variant="text"
                color="error"
                icon="mdi-delete"
                :loading="loadingConstraint"
                :aria-label="$t('pages.table.subpages.schema.constraints.delete.text')"
                :title="$t('pages.table.subpages.schema.constraints.delete.text')"
                @click="deleteConstraint({ type: 'unique', name: uniqueConstraint.name })" />
            </li>
            <li v-for="(checkConstraint, i) in table.constraints.checks" :key="`uk-${i}`">
              <strong
                class="text-uppercase"
                v-text="$t('pages.table.subpages.schema.check.label')" />
              (<i>{{ checkConstraint }}</i>)
              <v-btn
                v-if="canModifyConstraints"
                class="ml-1"
                density="compact"
                size="x-small"
                variant="text"
                color="error"
                icon="mdi-delete"
                :loading="loadingConstraint"
                :aria-label="$t('pages.table.subpages.schema.constraints.delete.text')"
                :title="$t('pages.table.subpages.schema.constraints.delete.text')"
                @click="deleteConstraint({ type: 'check', check: checkConstraint })" />
            </li>
          </ul>
        </v-container>
      </v-card-text>
    </v-card>
    <v-dialog
      v-model="dialogUnique"
      max-width="520">
      <v-card
        :title="$t('pages.table.subpages.schema.constraints.add_unique.text')">
        <v-card-text>
          <v-combobox
            v-model="uniqueColumnsDraft"
            multiple
            chips
            closable-chips
            :items="columnOptions"
            :variant="inputVariant"
            :label="$t('pages.table.subpages.schema.foreign_keys.columns.label')"
            :rules="[v => v.length > 0 || $t('validation.required')]" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            variant="text"
            :text="$t('navigation.cancel')"
            @click="dialogUnique = false" />
          <v-btn
            color="secondary"
            variant="flat"
            :loading="loadingConstraint"
            :disabled="uniqueColumnsDraft.length === 0"
            :text="$t('pages.table.subpages.schema.constraints.save.text')"
            @click="addUniqueConstraint" />
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog
      v-model="dialogForeignKey"
      max-width="1200">
      <v-card
        :title="$t('pages.table.subpages.schema.constraints.add_foreign_key.text')">
        <v-card-text>
          <TableForeignKeys
            v-model="foreignKeyDraft"
            :columns="table ? table.columns : []"
            :tables="database && database.tables ? database.tables : []"
            :current-table-name="table ? table.internal_name : null"
            :disabled="loadingConstraint"
            :input-variant="inputVariant"
            :button-variant="buttonVariant"
            :max="1" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            variant="text"
            :text="$t('navigation.cancel')"
            @click="dialogForeignKey = false" />
          <v-btn
            color="secondary"
            variant="flat"
            :loading="loadingConstraint"
            :disabled="!foreignKeyReady"
            :text="$t('pages.table.subpages.schema.constraints.save.text')"
            @click="addForeignKeyConstraint" />
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog
      v-model="dialogCheck"
      max-width="640">
      <v-card
        :title="$t('pages.table.subpages.schema.constraints.add_check.text')">
        <v-card-text>
          <v-text-field
            v-model="checkDraft"
            :variant="inputVariant"
            :label="$t('pages.table.subpages.schema.constraints.check_expression.label')"
            :rules="[v => !!v || $t('validation.required')]" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            variant="text"
            :text="$t('navigation.cancel')"
            @click="dialogCheck = false" />
          <v-btn
            color="secondary"
            variant="flat"
            :loading="loadingConstraint"
            :disabled="!checkDraft"
            :text="$t('pages.table.subpages.schema.constraints.save.text')"
            @click="addCheckConstraint" />
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog
      v-if="table && database"
      v-model="dialogSemantic"
      max-width="640">
      <DialogsSemantics
        :column="column"
        :table-id="table.id"
        :database="database"
        @close="closed" />
    </v-dialog>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script setup>
const { loggedIn } = useOidcAuth()
</script>
<script>
import TableForeignKeys from '@/components/table/TableForeignKeys.vue'
import TableToolbar from '@/components/table/TableToolbar.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    TableForeignKeys,
    TableToolbar
  },
  data () {
    return {
      selection: [],
      column: null,
      dialogSemantic: false,
      dialogUnique: false,
      dialogForeignKey: false,
      dialogCheck: false,
      uniqueColumnsDraft: [],
      foreignKeyDraft: [],
      checkDraft: '',
      loadingConstraint: false,
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
          title: this.$t('navigation.schema'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/schema`,
          disabled: true
        }
      ],
      headers: [
        { value: 'internal_name', title: this.$t('pages.table.subpages.schema.internal-name.title') },
        { value: 'type', title: this.$t('pages.table.subpages.schema.column-type.title') },
        { value: 'extra', title: this.$t('pages.table.subpages.schema.extra.title') },
        { value: 'column_concept', title: this.$t('pages.table.subpages.schema.concept.title') },
        { value: 'column_unit', title: this.$t('pages.table.subpages.schema.unit.title') },
        { value: 'description', title: this.$t('pages.table.subpages.schema.description.title') },
        { value: 'is_null_allowed', title: this.$t('pages.table.subpages.schema.nullable.title') },
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
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canViewSchema () {
      if (!this.table) {
        return false
      }
      if (this.table.is_schema_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    primaryKeysColumns () {
      return this.table.constraints.primary_key.map(pk => pk.column.internal_name).join(', ')
    },
    canAssignSemanticInformation () {
      if (!this.cacheUser || !this.roles) {
        return false
      }
      if (this.roles.includes('modify-foreign-table-column-semantics')) {
        return true
      }
      if (!this.access) {
        return false
      }
      return this.roles.includes('modify-table-column-semantics') && (this.access.type === 'write_all' || this.table.owner.username === this.cacheUser.preferred_username)
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    hasStructure () {
      const constraints = this.table.constraints
      return constraints.primary_key.length > 0 || constraints.foreign_keys.length > 0 || constraints.checks.length > 0 || constraints.uniques.length > 0
    },
    canModifyConstraints () {
      if (!this.table || !this.cacheUser || !this.roles || !this.roles.includes('update-table')) {
        return false
      }
      if (!this.access) {
        return false
      }
      if (this.access.type === 'write_all') {
        return true
      }
      return this.access.type === 'write_own' && this.table.owner.username === this.cacheUser.preferred_username
    },
    columnOptions () {
      if (!this.table || !this.table.columns) {
        return []
      }
      return this.table.columns.map(column => column.internal_name)
    },
    primaryKeyColumnNames () {
      if (!this.table || !this.table.constraints || !this.table.constraints.primary_key) {
        return []
      }
      return this.table.constraints.primary_key.map(pk => pk.column.internal_name)
    },
    foreignKeyReady () {
      if (this.foreignKeyDraft.length !== 1) {
        return false
      }
      const foreignKey = this.foreignKeyDraft[0]
      return foreignKey.columns.length > 0 && !!foreignKey.referenced_table && foreignKey.referenced_columns.length > 0
    }
  },
  methods: {
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
    hasUnit (item) {
      return !!this.unitUri(item)
    },
    hasConcept (item) {
      return !!this.conceptUri(item)
    },
    conceptUri (item) {
      return item.concept?.uri || item.concept_uri || null
    },
    unitUri (item) {
      return item.unit?.uri || item.unit_uri || null
    },
    pick (item) {
      this.column = item
      this.dialogSemantic = true
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
    openUniqueDialog () {
      this.uniqueColumnsDraft = []
      this.dialogUnique = true
    },
    openForeignKeyDialog () {
      this.foreignKeyDraft = [{
        columns: [],
        referenced_table: null,
        referenced_columns: [],
        on_update: 'no_action',
        on_delete: 'no_action'
      }]
      this.dialogForeignKey = true
    },
    openCheckDialog () {
      this.checkDraft = ''
      this.dialogCheck = true
    },
    addUniqueConstraint () {
      this.addConstraint({
        type: 'unique',
        columns: this.uniqueColumnsDraft
      }, () => {
        this.dialogUnique = false
      })
    },
    addForeignKeyConstraint () {
      const foreignKey = this.foreignKeyDraft[0]
      this.addConstraint({
        type: 'foreign_key',
        columns: foreignKey.columns,
        referenced_table: foreignKey.referenced_table,
        referenced_columns: foreignKey.referenced_columns,
        on_update: foreignKey.on_update || 'no_action',
        on_delete: foreignKey.on_delete || 'no_action'
      }, () => {
        this.dialogForeignKey = false
      })
    },
    addCheckConstraint () {
      this.addConstraint({
        type: 'check',
        check: this.checkDraft
      }, () => {
        this.dialogCheck = false
      })
    },
    toggleNull (column, nullAllowed) {
      const type = 'not_null'
      const tableService = useTableService()
      const action = nullAllowed ? tableService.removeConstraint : tableService.addConstraint
      this.loadingConstraint = true
      action(this.$route.params.database_id, this.$route.params.table_id, {
        type,
        column: column.internal_name
      })
        .then(this.constraintChanged)
        .catch(this.constraintFailed)
        .finally(() => {
          this.loadingConstraint = false
        })
    },
    addConstraint (data, success) {
      const tableService = useTableService()
      this.loadingConstraint = true
      tableService.addConstraint(this.$route.params.database_id, this.$route.params.table_id, data)
        .then(table => {
          this.constraintChanged(table)
          success()
        })
        .catch(this.constraintFailed)
        .finally(() => {
          this.loadingConstraint = false
        })
    },
    deleteConstraint (data) {
      const tableService = useTableService()
      this.loadingConstraint = true
      tableService.removeConstraint(this.$route.params.database_id, this.$route.params.table_id, data)
        .then(this.constraintChanged)
        .catch(this.constraintFailed)
        .finally(() => {
          this.loadingConstraint = false
        })
    },
    constraintChanged (table) {
      const toast = useToastInstance()
      toast.success(this.$t('success.table.constraints'))
      this.cacheStore.setTable(table)
      this.cacheStore.reloadDatabase()
    },
    constraintFailed ({code, message}) {
      const toast = useToastInstance()
      if (typeof code !== 'string') {
        toast.error(message)
        return
      }
      toast.error(this.$t(code))
    },
    isPrimaryKeyColumn (column) {
      return this.primaryKeyColumnNames.includes(column.internal_name)
    },
    foreignKeyColumns (foreignKey) {
      if (!foreignKey) {
        return null
      }
      return foreignKey.references.map(r => r.column.internal_name).join(',')
    },
    foreignKeyReferencedTable (foreignKey) {
      if (!foreignKey) {
        return null
      }
      return foreignKey.referenced_table.internal_name
    },
    foreignKeyReferencedColumns (foreignKey) {
      if (!foreignKey) {
        return null
      }
      return foreignKey.references.map(r => r.referenced_column.internal_name).join(',')
    },
    uniqueColumns (uniqueConstraint) {
      if (!uniqueConstraint) {
        return null
      }
      return uniqueConstraint.columns.map(c => c.internal_name).join(',')
    },
    tooltip (item, mode) {
      if (!item[mode]) {
        return null
      }
      if (item[mode].name) {
        return item[mode].name
      }
      if (item[mode].description) {
        return item[mode].description
      }
      return null
    }
  }
}
</script>
