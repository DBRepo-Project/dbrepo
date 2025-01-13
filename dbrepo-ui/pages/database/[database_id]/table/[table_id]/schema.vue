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
          <v-btn
            v-if="canAssignSemanticInformation && !hasConcept(item)"
            size="small"
            color="tertiary"
            :variant="buttonVariant"
            :text="$t('pages.table.subpages.schema.assign')"
            @click="pick(item, 'concept')" />
          <v-btn
            v-if="canAssignSemanticInformation && hasConcept(item)"
            :title="item.concept.uri"
            color="tertiary"
            :variant="buttonVariant"
            size="small"
            :text="item.concept.name ? item.concept.name : item.concept.uri"
            @click="pick(item, 'concept')" />
          <a
            v-if="!canAssignSemanticInformation && hasConcept(item)"
            :href="item.concept.uri">
            {{ item.concept.name ? item.concept.name : item.concept.uri }}
          </a>
        </template>
        <template v-slot:item.column_unit="{ item }">
          <v-btn
            v-if="canAssignSemanticInformation && !hasUnit(item)"
            size="small"
            color="tertiary"
            :variant="buttonVariant"
            :text="$t('pages.table.subpages.schema.assign')"
            @click="pick(item, 'unit')" />
          <v-btn
            v-if="canAssignSemanticInformation && hasUnit(item)"
            :title="item.unit.uri"
            color="tertiary"
            :variant="buttonVariant"
            size="small"
            :text="item.unit.name ? item.unit.name : item.unit.uri"
            @click="pick(item, 'unit')" />
          <a
            v-if="!canAssignSemanticInformation && hasUnit(item)"
            :href="item.unit.uri">
            {{ item.unit.name ? item.unit.name : item.unit.uri }}
          </a>
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
              <strong>PRIMARY KEY</strong>
              (<i>{{ primaryKeysColumns }}</i>)
            </li>
            <li v-for="(foreignKey, i) in table.constraints.foreign_keys" :key="`fk-${i}`">
              <strong>FOREIGN KEY</strong> <span>{{ foreignKey.name }}</span> (<i>{{ foreignKeyColumns(foreignKey) }}</i>) <strong>REFERENCES</strong> <a :href="`/database/${database.id}/table/${foreignKey.referenced_table.id}/schema`">{{ foreignKeyReferencedTable(foreignKey) }}</a> (<i>{{ foreignKeyReferencedColumns(foreignKey) }}</i>)
            </li>
            <li v-for="(uniqueConstraint, i) in table.constraints.uniques" :key="`uk-${i}`">
              <strong>UNIQUE INDEX</strong>
              (<i>{{ uniqueColumns(uniqueConstraint) }}</i>)
            </li>
            <li v-for="(checkConstraint, i) in table.constraints.checks" :key="`uk-${i}`">
              <strong>CHECK CONSTRAINT</strong>
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
        :mode="mode"
        :table-id="table.id"
        :database="database"
        @close="closed" />
    </v-dialog>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'table' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'table' })" />
</template>

<script>
import TableToolbar from '@/components/table/TableToolbar.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import { errorCodeKey } from '@/utils'

export default {
  components: {
    TableToolbar
  },
  setup () {
    const config = useRuntimeConfig()
    const userStore = useUserStore()
    const { database_id, table_id } = useRoute().params
    const { error, data } = useFetch(`${config.public.api.server}/api/database/${database_id}/table/${table_id}`, {
      immediate: true,
      timeout: 90_000,
      headers: {
        Accept: 'application/json',
        Authorization: userStore.getToken ? `Bearer ${userStore.getToken}` : null
      }
    })
    if (data.value) {
      const identifierService = useIdentifierService()
      useServerHead(identifierService.databaseToServerHead(data.value))
      useServerSeoMeta(identifierService.databaseToServerSeoMeta(data.value))
    }
    return {
      error
    }
  },
  data () {
    return {
      selection: [],
      column: null,
      mode: null,
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
        { value: 'is_null_allowed', title: this.$t('pages.table.subpages.schema.nullable.title') },
        { value: 'description', title: this.$t('pages.table.subpages.schema.description.title') },
      ],
      dateColumns: [],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    table () {
      return this.cacheStore.getTable
    },
    access () {
      return this.userStore.getAccess
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    roles () {
      return this.userStore.getRoles
    },
    canViewSchema () {
      if (this.error) {
        return false
      }
      if (!this.table) {
        return false
      }
      if (this.table.is_schema_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.hasReadAccess || this.table.owner.id === this.user.id || this.database.owner.id === this.user.id
    },
    primaryKeysColumns () {
      return this.table.constraints.primary_key.map(pk => pk.column.internal_name).join(', ')
    },
    canAssignSemanticInformation () {
      if (!this.user) {
        return false
      }
      if (this.roles.includes('modify-foreign-table-column-semantics')) {
        return true
      }
      if (!this.access) {
        return false
      }
      return this.roles.includes('modify-table-column-semantics') && (this.access.type === 'write_all' || this.table.owner.username === this.user.username)
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
    errorCodeKey,
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
      return item.unit && 'uri' in item.unit
    },
    hasConcept (item) {
      return item.concept && 'uri' in item.concept
    },
    pick (item, mode) {
      this.column = item
      this.mode = mode
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
    }
  }
}
</script>
