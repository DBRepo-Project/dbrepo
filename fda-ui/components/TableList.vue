<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && tables.length === 0" flat>
      <v-card-title>
        (no tables)
      </v-card-title>
    </v-card>
    <v-expansion-panels v-if="!loading && tables.length > 0" v-model="panel" accordion>
      <v-expansion-panel v-for="(item,i) in tables" :key="i" @click="details(item.id)">
        <v-expansion-panel-header>
          {{ item.name }}
        </v-expansion-panel-header>
        <v-expansion-panel-content class="mb-2">
          <v-row dense>
            <v-col>
              <v-list dense>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Table ID
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ tableDetails.id }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Table Internal Name
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ tableDetails.internal_name }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      AMQP Exchange
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ database.exchange }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      AMQP Routing Key
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ tableDetails.topic }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      AMQP Consumer(s)
                    </v-list-item-title>
                    <v-list-item-content class="amqp-consumer">
                      {{ tableDetails.consumers.length }} <v-badge
                        v-if="!tableDetails.consumersUp"
                        class="ml-1"
                        color="error"
                        content="down" />
                      <v-badge
                        v-if="tableDetails.consumersUp"
                        class="ml-1"
                        color="success"
                        content="up" />
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Table Creation
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ createdUTC }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Description
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ tableDetails.description }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
              </v-list>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-btn color="secondary" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${item.id}`">
                View Data
              </v-btn>
              <v-btn color="secondary" class="ml-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create?tid=${item.id}`">
                Create Subset
              </v-btn>
              <v-btn class="ml-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${item.id}/import`">
                Import csv
              </v-btn>
            </v-col>
            <v-col class="align-right">
              <v-btn v-if="false" outlined color="error" @click="showDeleteTableDialog(item.id)">
                Delete
              </v-btn>
            </v-col>
          </v-row>
          <v-row v-if="tableDetails.columns">
            <v-data-table
              class="full-width"
              disable-sort
              :loading="loadingDetails"
              hide-default-footer
              :headers="headers"
              :items="tableDetails.columns">
              <template v-slot:item.is_null_allowed="{ item }">
                <span v-if="item.is_null_allowed">●</span> {{ item.is_null_allowed }}
              </template>
              <template v-slot:item.unique="{ item }">
                <span v-if="item.unique">●</span> {{ item.unique }}
              </template>
              <template v-slot:item.column_type="{ item }">
                {{ columnName(item) }}
              </template>
              <template v-slot:item.is_primary_key="{ item }">
                <span v-if="item.is_primary_key">●</span> {{ item.is_primary_key }}
              </template>
              <template v-slot:item.auto_generated="{ item }">
                <span v-if="item.auto_generated">●</span> {{ item.auto_generated }}
              </template>
              <template v-slot:item.column_concept="{ item }">
                <v-btn v-if="!item.column_concept" small @click="pickUnit(item)">Assign</v-btn>
                <v-btn
                  v-if="item.column_concept"
                  :title="item.column_concept.uri"
                  color="secondary"
                  small
                  @click="pickUnit(item)">
                  {{ item.column_concept.name }}
                </v-btn>
              </template>
            </v-data-table>
          </v-row>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
    <v-dialog
      v-model="unitDialog"
      max-width="600px">
      <DialogsColumnUnit :column="column" :table-id="tableDetails.id" @close="closed" />
    </v-dialog>
    <v-dialog v-model="dialogDelete" max-width="640">
      <v-card>
        <v-card-title class="headline">
          Delete
        </v-card-title>
        <v-card-text class="pb-1">
          Are you sure you want to drop this table?
        </v-card-text>
        <v-card-actions class="pl-4 pb-4 pr-4">
          <v-btn @click="dialogDelete = false">
            Cancel
          </v-btn>
          <v-spacer />
          <v-btn color="error" @click="deleteTable()">
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
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
      tables: [],
      panel: null,
      column: null,
      unitDialog: false,
      database: {
        exchange: null,
        tables: []
      },
      tableDetails: {
        id: null,
        internal_name: null,
        description: null,
        topic: null,
        columns: [],
        created: null,
        consumers: [],
        consumersUp: false
      },
      dialogDelete: false,
      headers: [
        { value: 'name', text: 'Name' },
        { value: 'column_type', text: 'Type' },
        { value: 'column_concept', text: 'Unit of Measurement' },
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
        { value: 'decimal', text: 'Decimal' },
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
    brokerConfig () {
      return {
        headers: { Authorization: 'Basic ' + btoa(`${this.$config.brokerUsername}:${this.$config.brokerPassword}`) }
      }
    },
    createdUTC () {
      return formatTimestampUTCLabel(this.tableDetails.created)
    }
  },
  mounted () {
    this.$root.$on('table-create', this.refresh)
    this.loadDatabase()
  },
  methods: {
    pickUnit (item) {
      this.column = item
      this.unitDialog = true
      console.debug('select', this.unit)
    },
    async loadDatabase () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', this.database)
        this.tables = this.database.tables
        console.debug('tables', this.tables)
      } catch (err) {
        this.error = true
        this.$toast.error('Could not get database details.')
      }
      this.loading = false
    },
    columnName (column) {
      const filter = this.columnTypes.filter(t => t.value === column.column_type)
      if (filter.length > 0) {
        return filter[0].text
      }
      return column.column_type
    },
    async details (tableId) {
      try {
        this.loadingDetails = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${tableId}`, this.config)
        this.tableDetails = res.data
        console.debug('table details', this.tableDetails)
        if (tableId) {
          this.openPanelByTableId(tableId)
          await this.consumerDetails(this.tableDetails.topic)
        }
      } catch (err) {
        this.$toast.error('Failed to load table details')
        console.error('Failed to load table details', err)
      }
      this.loadingDetails = false
    },
    closed (data) {
      console.debug('closed dialog', data)
      this.unitDialog = false
    },
    /**
     * if tableId is given, open the table after refresh
     */
    async refresh (tableId) {
      let res
      try {
        this.loading = true
        res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, this.config)
        this.tables = res.data
        this.loading = false
        if (tableId) { this.openPanelByTableId(tableId) }
      } catch (err) {
        this.$toast.error('Could not load tables.')
      }
      this.$store.commit('SET_TABLE', null)
    },
    async deleteTable () {
      try {
        this.loading = true
        await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.deleteTableId}`, this.config)
        this.loading = false
        this.refresh()
      } catch (err) {
        this.$toast.error('Could not delete table.')
      }
      this.dialogDelete = false
    },
    async consumerDetails (topic) {
      try {
        this.loading = true
        const res = await this.$axios.get('/api/broker/consumers/%2F', this.brokerConfig)
        const consumers = res.data.filter(c => c.queue.name === topic)
        console.debug('consumers', consumers)
        const state = res.data.filter(c => c.queue.name === topic && c.active)
        this.tableDetails.consumers = consumers
        this.tableDetails.consumersUp = consumers.length === state.length
        this.loading = false
      } catch (err) {
        this.$toast.error('Could not find consumers.')
      }
      this.dialogDelete = false
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

<style>
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
