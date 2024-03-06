<template>
  <div>
    <TableToolbar :selection="selection" />
    <v-toolbar color="secondary white--text" flat>
      <strong>
        <v-toolbar-title>Schema</v-toolbar-title>
      </strong>
    </v-toolbar>
    <v-card tile>
      <v-data-table
        v-if="table"
        class="full-width"
        disable-sort
        hide-default-footer
        :items-per-page="-1"
        :headers="headers"
        :items="table.columns">
        <template v-slot:item.is_null_allowed="{ item }">
          <span v-if="item.is_null_allowed">●</span> {{ item.is_null_allowed }}
        </template>
        <template v-slot:item.unique="{ item }">
          <span v-if="isUnique(item)">●</span> {{ isUnique(item) }}
        </template>
        <template v-slot:item.extra="{ item }">
          <pre>{{ extra(item) }}</pre>
        </template>
        <template v-slot:item.is_primary_key="{ item }">
          <span v-if="item.is_primary_key">●</span> {{ item.is_primary_key }}
        </template>
        <template v-slot:item.auto_generated="{ item }">
          <span v-if="item.auto_generated">●</span> {{ item.auto_generated }}
        </template>
        <template v-slot:item.column_concept="{ item }">
          <v-btn v-if="canAssignSemanticInformation && !hasConcept(item)" small @click="pick(item, 'concept')">Assign</v-btn>
          <v-btn
            v-if="canAssignSemanticInformation && hasConcept(item)"
            :title="item.concept.uri"
            color="secondary"
            small
            @click="pick(item, 'concept')">
            <span v-if="item.concept.name" v-text="item.concept.name" />
            <span v-else v-text="item.concept.uri" />
          </v-btn>
          <a v-if="!canAssignSemanticInformation && hasConcept(item)" :href="item.concept.uri" target="_blank">
            <span v-if="item.concept.name" v-text="item.concept.name" />
            <span v-else v-text="item.concept.uri" />
          </a>
        </template>
        <template v-slot:item.column_unit="{ item }">
          <v-btn v-if="canAssignSemanticInformation && !hasUnit(item)" small @click="pick(item, 'unit')">Assign</v-btn>
          <v-btn
            v-if="canAssignSemanticInformation && hasUnit(item)"
            :title="item.unit.uri"
            color="secondary"
            small
            @click="pick(item, 'unit')">
            <span v-if="item.unit.name" v-text="item.unit.name" />
            <span v-else v-text="item.unit.uri" />
          </v-btn>
          <a v-if="!canAssignSemanticInformation && hasUnit(item)" :href="item.unit.uri" target="_blank">
            <span v-if="item.unit.name" v-text="item.unit.name" />
            <span v-else v-text="item.unit.uri" />
          </a>
        </template>
      </v-data-table>
    </v-card>
    <v-card v-if="hasConstraints" tile>
      <v-card-subtitle>Constraints</v-card-subtitle>
      <v-card-text>
        <ul>
          <li v-for="(foreignKey,i) in table.constraints.foreign_keys" :key="`fk-${i}`">
            <strong>FOREIGN KEY</strong>
            <span v-text="foreignKey.name" />
            (<i v-text="foreignKeyColumns(foreignKey)" />)
            <strong>REFERENCES</strong>
            <a :href="`/database/${database.id}/table/${foreignKey.referenced_table.id}/schema`" v-text="foreignKeyReferencedTable(foreignKey)" />
            (<i v-text="foreignKeyReferencedColumns(foreignKey)" />)
          </li>
          <li v-for="(uniqueConstraint,i) in table.constraints.uniques" :key="`uk-${i}`">
            <strong>UNIQUE INDEX</strong>
            (<i v-text="uniqueColumns(uniqueConstraint)" />)
          </li>
          <li v-for="(checkConstraint,i) in table.constraints.checks" :key="`uk-${i}`">
            <strong>CHECK CONSTRAINT</strong>
            (<i v-text="checkConstraint" />)
          </li>
        </ul>
      </v-card-text>
    </v-card>
    <v-dialog
      v-if="table && database"
      v-model="dialogSemantic"
      persistent
      max-width="640">
      <DialogsSemantics
        :column="column"
        :mode="mode"
        :table-id="table.id"
        :database="database"
        @close="closed" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableToolbar from '@/components/table/TableToolbar.vue'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      selection: [],
      column: null,
      mode: null,
      dialogSemantic: false,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}/info`, activeClass: '' },
        { text: 'Tables', to: `/database/${this.$route.params.database_id}/table`, activeClass: '' },
        { text: `${this.$route.params.table_id}`, to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, activeClass: '' }
      ],
      headers: [
        { value: 'internal_name', text: 'Column Name' },
        { value: 'column_type', text: 'Type' },
        { value: 'extra', text: 'Extra Information' },
        { value: 'column_concept', text: 'Concept' },
        { value: 'column_unit', text: 'Unit' },
        { value: 'is_primary_key', text: 'Primary Key' },
        { value: 'is_null_allowed', text: 'Nullable' },
        { value: 'auto_generated', text: 'Sequence' }
      ],
      dateColumns: []
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
    },
    access () {
      return this.$store.state.access
    },
    roles () {
      return this.$store.state.roles
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
    hasConstraints () {
      if (!this.table || !this.table.constraints) {
        return false
      }
      return this.table.constraints.uniques.length > 0 || this.table.constraints.checks.length > 0 || this.table.constraints.foreign_keys.length > 0
    }
  },
  mounted () {
    this.$store.dispatch('reloadOntologies')
  },
  methods: {
    isUnique (column) {
      if (!this.table || !this.table.constraints || !this.table.constraints.uniques) {
        return false
      }
      const uniqueColumnIds = this.table.constraints.uniques.map(u => u.columns.map(c => c.id)).flat()
      return uniqueColumnIds.includes(column.id)
    },
    extra (column) {
      if (['date', 'datetime', 'timestamp', 'time'].includes(column.column_type)) {
        return `fsp=${column.date_format.unix_format}`
      } else if (column.column_type === 'float') {
        return `p=${column.size}`
      } else if (['decimal', 'double'].includes(column.column_type)) {
        return `size=${column.size} d=${column.d}`
      } else if (column.column_type === 'enum') {
        return `(${column.enums.join(', ')})`
      } else if (column.column_type === 'set') {
        return `(${column.sets.join(', ')})`
      } else if (['int', 'char', 'varchar', 'binary', 'varbinary', 'tinyint', 'smallint', 'mediumint', 'bigint'].includes(column.column_type)) {
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
        this.$store.dispatch('reloadTable')
      }
      this.dialogSemantic = false
    },
    foreignKeyColumns (foreignKey) {
      if (!foreignKey) {
        return null
      }
      return foreignKey.columns.map(c => c.internal_name).join(',')
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
      return foreignKey.referenced_columns.map(c => c.internal_name).join(',')
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
