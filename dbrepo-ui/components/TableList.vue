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
        <v-list-item two-line :class="clazz(item)" :to="`/database/${$route.params.database_id}/table/${item.id}/info`">
          <v-list-item-content>
            <v-list-item-title v-text="item.name" />
            <v-list-item-subtitle class="mt-2">
              <span v-if="item.description" v-text="item.description" />
              <span v-else>(no description)</span>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action v-if="item.identifiers && item.identifiers.length > 0">
            <v-tooltip left>
              <template v-slot:activator="{ on, attrs }">
                <v-icon color="primary" v-bind="attrs" v-on="on">mdi-identifier</v-icon>
              </template>
              Persistent identifier
            </v-tooltip>
          </v-list-item-action>
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
    access () {
      return this.$store.state.access
    },
    tables () {
      if (!this.database) {
        return null
      }
      return this.database.tables
    }
  },
  methods: {
    pick (item, mode) {
      this.column = item
      this.mode = mode
      this.dialogSemantic = true
    },
    clazz (table) {
      return this.hasIdentifiers(table) ? 'primary--text' : null
    },
    hasIdentifiers (table) {
      return table && 'identifiers' in table && table.identifiers.length > 0
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
