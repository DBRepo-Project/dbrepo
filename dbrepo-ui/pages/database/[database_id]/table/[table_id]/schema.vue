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
          <span
            v-if="item.is_null_allowed">
            {{ $t('pages.table.subpages.schema.bullet') }}
          </span>
          {{ item.is_null_allowed }}
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
              <span>{{ foreignKey.name }}</span> (<i>{{ foreignKeyColumns(foreignKey) }}</i>) <strong>REFERENCES</strong> <a :href="`/database/${database.id}/table/${foreignKey.referenced_table.id}/schema`">{{ foreignKeyReferencedTable(foreignKey) }}</a> (<i>{{ foreignKeyReferencedColumns(foreignKey) }}</i>)
            </li>
            <li v-for="(uniqueConstraint, i) in table.constraints.uniques" :key="`uk-${i}`">
              <strong
                class="text-uppercase"
                v-text="$t('pages.table.subpages.schema.unique.label')" />
              (<i>{{ uniqueColumns(uniqueConstraint) }}</i>)
            </li>
            <li v-for="(checkConstraint, i) in table.constraints.checks" :key="`uk-${i}`">
              <strong
                class="text-uppercase"
                v-text="$t('pages.table.subpages.schema.check.label')" />
              (<i>{{ checkConstraint }}</i>)
            </li>
          </ul>
        </v-container>
      </v-card-text>
    </v-card>
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
import TableToolbar from '@/components/table/TableToolbar.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      selection: [],
      column: null,
      dialogSemantic: false,
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
