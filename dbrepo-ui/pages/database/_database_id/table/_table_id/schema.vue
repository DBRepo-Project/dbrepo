<template>
  <div>
    <TableToolbar :selection="selection" />
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
      <template v-slot:item.column_type="{ item }">
        {{ columnName(item) }}
      </template>
      <template v-slot:item.date_format="{ item }">
        {{ dateFormat(item) }}
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
import TableToolbar from '@/components/TableToolbar.vue'
import TableService from '@/api/table.service'

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
        { value: 'date_format', text: 'Date Format' },
        { value: 'column_concept', text: 'Concept' },
        { value: 'column_unit', text: 'Unit' },
        { value: 'is_primary_key', text: 'Primary Key' },
        { value: 'unique', text: 'Unique' },
        { value: 'is_null_allowed', text: 'Nullable' },
        { value: 'auto_generated', text: 'Sequence' }
      ],
      columnTypes: [
        // { value: 'ENUM', text: 'Enumeration' }, // Disabled for now, not implemented, #145
        { value: 'boolean', text: 'Boolean' },
        { value: 'number', text: 'Number' },
        { value: 'blob', text: 'Binary Large Object' },
        { value: 'date', text: 'Date' },
        { value: 'timestamp', text: 'Timestamp' },
        { value: 'decimal', text: 'Floating Number' },
        { value: 'string', text: 'Character Varying' },
        { value: 'text', text: 'Text' }
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
    versionColor () {
      if (this.version === null) {
        return 'secondary white--text'
      }
      return 'primary white--text'
    },
    versionFormatted () {
      if (this.version === null) {
        return null
      }
      return this.version + ' (UTC)'
    },
    versionISO () {
      if (this.version === null) {
        return null
      }
      return this.version.substring(0, 10) + 'T' + this.version.substring(11, 19) + 'Z'
    }
  },
  mounted () {
    this.$store.dispatch('reloadOntologies')
  },
  methods: {
    isUnique (column) {
      return !!this.table.constraints?.uniques?.some(uniqueCols => [...uniqueCols].every(unique => unique.id === column.id))
    },
    columnName (column) {
      const filter = this.columnTypes.filter(t => t.value === column.column_type)
      if (filter.length > 0) {
        return filter[0].text
      }
      return column.column_type
    },
    dateFormat (column) {
      if (column.date_format) {
        return column.date_format.unix_format
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
    loadTable () {
      if (!this.$route.params.database_id || !this.$route.params.table_id) {
        return
      }
      this.loading = true
      TableService.findOne(this.$route.params.database_id, this.$route.params.table_id)
        .then((table) => {
          this.$store.commit('SET_TABLE', table)
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
