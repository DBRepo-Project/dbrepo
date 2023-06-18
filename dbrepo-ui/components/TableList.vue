<template>
  <div>
    <v-progress-linear v-if="loading" indeterminate />
    <v-card v-if="!loading && tables && tables.length === 0" flat>
      <v-card-text>
        (no tables)
      </v-card-text>
    </v-card>
    <div v-for="(item,i) in tables" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list-item-group>
        <v-list-item
          two-line
          :to="`/database/${$route.params.database_id}/table/${item.id}`">
          <v-list-item-content>
            <v-list-item-title v-text="item.name" />
            <v-list-item-subtitle class="mt-2" v-text="item.description" />
          </v-list-item-content>
        </v-list-item>
      </v-list-item-group>
    </div>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'

export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      error: false,
      panel: null,
      column: null,
      dialogSemantic: false,
      mode: 'unit',
      access: {
        type: null
      },
      tableDetails: {
        id: null,
        internal_name: null,
        description: null,
        queue_name: null,
        routing_key: null,
        columns: [],
        created: null,
        creator: {
          username: null
        }
      },
      dialogDelete: false,
      headers: [
        { value: 'name', text: 'Name' },
        { value: 'column_type', text: 'Type' },
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
      ]
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    tables () {
      if (!this.database) {
        return null
      }
      return this.database.tables
    },
    createdUTC () {
      if (this.tableDetails.created === undefined || this.tableDetails.created === null) {
        return null
      }
      return formatTimestampUTCLabel(this.tableDetails.created)
    },
    canRead () {
      if (this.database?.is_public) {
        return true
      }
      if (!this.user || !this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
    },
    canModify () {
      if (!this.token || !this.user.username) {
        /* not yet loaded */
        return false
      }
      return this.database.creator.username === this.user.username
    }
  },
  methods: {
    pick (item, mode) {
      this.column = item
      this.mode = mode
      this.dialogSemantic = true
    },
    hasUnit (item) {
      return item.unit !== null
    },
    hasConcept (item) {
      return item.concept !== null
    },
    columnName (column) {
      const filter = this.columnTypes.filter(t => t.value === column.column_type)
      if (filter.length > 0) {
        return filter[0].text
      }
      return column.column_type
    },
    closed (data) {
      console.debug('closed dialog', data)
      this.dialogSemantic = false
    },
    created (created) {
      return formatTimestampUTCLabel(created)
    }
  }
}
</script>

<style scoped>
.colTable thead th {
  text-align: initial;
}
.colTable tbody tr td {
  padding-left: 0;
}
.align-right {
  text-align: right;
}
.full-width {
  width: 100%;
}
</style>
