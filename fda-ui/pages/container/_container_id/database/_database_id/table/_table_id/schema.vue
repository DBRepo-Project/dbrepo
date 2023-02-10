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
        <span v-if="item.unique">●</span> {{ item.unique }}
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
        <v-btn v-if="canModify && !hasConcept(item)" small @click="pick(item, 'concept')">Assign</v-btn>
        <v-btn
          v-if="canModify && hasConcept(item)"
          :title="item.concept.uri"
          color="secondary"
          small
          @click="pick(item, 'concept')">
          {{ item.concept.name }}
        </v-btn>
        <a v-if="!canModify && hasConcept(item)" :href="item.concept.uri" target="_blank">
          {{ item.concept.name }}
        </a>
      </template>
      <template v-slot:item.column_unit="{ item }">
        <v-btn v-if="canModify && !hasUnit(item)" small @click="pick(item, 'unit')">Assign</v-btn>
        <v-btn
          v-if="canModify && hasUnit(item)"
          :title="item.unit.uri"
          color="secondary"
          small
          @click="pick(item, 'unit')">
          {{ item.unit.name }}
        </v-btn>
        <a v-if="!canModify && hasUnit(item)" :href="item.unit.uri" target="_blank">
          {{ item.unit.name }}
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
        :table-id="table.id"
        :database="database"
        @close="closed" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableToolbar from '@/components/TableToolbar'

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
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`, activeClass: '' },
        { text: 'Tables', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, activeClass: '' },
        { text: `${this.$route.params.table_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, activeClass: '' }
      ],
      headers: [
        { value: 'name', text: 'Name' },
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
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {
          headers: {},
          progress: false
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
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
    canModify () {
      if (!this.token || !this.user.username) {
        /* not yet loaded */
        return false
      }
      return this.table.creator.username === this.user.username
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
  },
  methods: {
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
      return item.unit !== null
    },
    hasConcept (item) {
      return item.concept !== null
    },
    pick (item) {
      this.column = item
      this.dialogSemantic = true
    },
    closed (event) {
      const { success } = event
      console.debug('closed dialog', event)
      if (success) {
        this.loadTable()
      }
      this.dialogSemantic = false
    },
    async loadTable () {
      if (!this.$route.params.container_id || !this.$route.params.database_id || !this.$route.params.table_id) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, this.config)
        this.$store.commit('SET_TABLE', res.data)
        console.debug('table', this.table)
      } catch (err) {
        console.error('Could not load table', err)
        this.$toast.error('Could not load table')
      }
      this.loading = false
    }
  }
}
</script>
