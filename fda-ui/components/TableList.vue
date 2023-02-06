<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && tables.length === 0" flat>
      <v-card-text>
        (no tables)
      </v-card-text>
    </v-card>
    <div v-for="(item,i) in tables" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list-item-group>
        <v-list-item two-line :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${item.id}`">
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
      loadingConsumers: false,
      error: false,
      panel: null,
      column: null,
      dialogSemantic: false,
      mode: 'unit',
      consumers: [],
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
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    config () {
      if (this.token === null) {
        return {}
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
    tables () {
      if (!this.database) {
        return []
      }
      return this.database.tables
    },
    brokerConfig () {
      return {
        headers: { Authorization: 'Basic ' + btoa(`${this.$config.brokerUsername}:${this.$config.brokerPassword}`) },
        progress: false
      }
    },
    createdUTC () {
      if (this.tableDetails.created === undefined || this.tableDetails.created === null) {
        return null
      }
      return formatTimestampUTCLabel(this.tableDetails.created)
    },
    hasReadAccess () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        /* database is public */
        return true
      }
      if (!this.user) {
        return false
      }
      if (this.database.creator.username === this.user.username) {
        /* user is creator of database */
        return true
      }
      if (!this.access) {
        return false
      }
      if (this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all') {
        /* user has some level of access */
        return true
      }
      return false
    },
    canModify () {
      if (!this.token || !this.user.username) {
        /* not yet loaded */
        return false
      }
      return this.database.creator.username === this.user.username
    }
  },
  mounted () {
    this.pollConsumerStatus()
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
    async details (table) {
      /* use cache */
      this.tableDetails = table
      /* load remaining info */
      if (this.hasReadAccess) {
        try {
          this.loadingDetails = true
          const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${table.id}`, this.config)
          this.tableDetails = res.data
          console.debug('table details', this.tableDetails)
          if (table.id) {
            this.openPanelByTableId(table.id)
            await this.consumerDetails(this.tableDetails.queue_name)
          }
        } catch (err) {
          this.$toast.error('Failed to load table details')
          console.error('Failed to load table details', err)
        }
        this.loadingDetails = false
      }
    },
    is_owner (table) {
      if (!this.user) {
        return false
      }
      return table.creator.username === this.user.username
    },
    closed (data) {
      console.debug('closed dialog', data)
      this.dialogSemantic = false
    },
    created (created) {
      return formatTimestampUTCLabel(created)
    },
    async deleteTable () {
      try {
        this.loading = true
        await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.deleteTableId}`, this.config)
        this.loading = false
        this.refresh()
      } catch (err) {
        this.$toast.error('Could not delete table')
      }
      this.dialogDelete = false
    },
    async consumerDetails (queueName) {
      try {
        this.loadingConsumers = true
        const res = await this.$axios.get('/api/broker/consumers/%2F', this.brokerConfig)
        const consumers = res.data.filter(c => c.queue.name === queueName)
        console.debug('consumers', consumers)
        this.consumers = consumers
      } catch (err) {
        console.error('Could not find consumers', err)
      }
      this.loadingConsumers = false
    },
    pollConsumerStatus () {
      if (this.tableDetails === undefined || this.tableDetails.queue_name === undefined) {
        return
      }
      this.consumerDetails(this.tableDetails.queue_name)
    },
    showDeleteTableDialog (id) {
      this.deleteTableId = id
      this.dialogDelete = true
    },
    /**
     * open up the accordion with the table that has been updated (by the ColumnUnit dialog)
     */
    openPanelByTableId (id) {
      this.panel = this.tables.findIndex(t => t.id === id)
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
.amqp-consumer {
  display: inline;
}
</style>
