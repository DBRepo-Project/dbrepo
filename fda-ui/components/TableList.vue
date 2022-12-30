<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && tables.length === 0" flat>
      <v-card-text>
        (no tables)
      </v-card-text>
    </v-card>
    <v-expansion-panels v-if="!loading && tables.length > 0" v-model="panel" accordion flat>
      <v-expansion-panel v-for="(item,i) in tables" :key="i" @click="details(item)">
        <v-expansion-panel-header>
          <span>{{ item.name }}</span>
        </v-expansion-panel-header>
        <v-expansion-panel-content class="mb-2">
          <v-row v-if="loadingDetails" dense>
            <v-progress-linear color="primary" indeterminate />
          </v-row>
          <v-row v-if="!loadingDetails" dense>
            <v-col>
              <v-list dense>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Table ID
                    </v-list-item-title>
                    <v-list-item-content v-text="tableDetails.id " />
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Table Description
                    </v-list-item-title>
                    <v-list-item-content v-text="tableDetails.description" />
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Exchange Name (AMQP/MQTT)
                    </v-list-item-title>
                    <v-list-item-content v-text="database.exchange_name" />
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Queue Name (AMQP/MQTT)
                    </v-list-item-title>
                    <v-list-item-content v-text="tableDetails.queue_name" />
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Routing Key (AMQP/MQTT)
                    </v-list-item-title>
                    <v-list-item-content>
                      <span v-text="tableDetails.routing_key" />
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item v-if="hasReadAccess">
                  <v-list-item-content>
                    <v-list-item-title>
                      AMQP Consumer(s)
                    </v-list-item-title>
                    <v-list-item-content class="amqp-consumer">
                      <span v-if="justCreated">Creating consumers ...</span>
                      <span v-if="!justCreated" v-text="`${consumersUp}/${consumersTotal}`" />
                      <v-badge
                        v-if="!justCreated"
                        class="ml-1"
                        :color="consumersState.color"
                        :content="consumersState.text" />
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    <v-list-item-title>
                      Table Creator
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ formatCreator(item.creator) }}
                      <span v-if="is_owner(item)" style="flex:none;">&nbsp;(you)</span>
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item v-if="createdUTC">
                  <v-list-item-content>
                    <v-list-item-title>
                      Table Creation
                    </v-list-item-title>
                    <v-list-item-content v-text="createdUTC" />
                  </v-list-item-content>
                </v-list-item>
              </v-list>
            </v-col>
          </v-row>
          <v-row v-if="hasReadAccess" dense>
            <v-col>
              <v-btn small color="secondary" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${item.id}`">
                View Data
              </v-btn>
              <v-btn v-if="canModify" small color="secondary" class="ml-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create?tid=${item.id}`">
                Create Subset
              </v-btn>
              <v-btn v-if="canModify" small class="ml-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${item.id}/import`">
                Import csv
              </v-btn>
            </v-col>
            <v-col v-if="false" class="align-right">
              <v-btn outlined color="error" @click="showDeleteTableDialog(item.id)">
                Delete
              </v-btn>
            </v-col>
          </v-row>
          <v-row v-if="hasReadAccess && tableDetails.columns">
            <v-data-table
              class="full-width"
              disable-sort
              :loading="loadingDetails"
              hide-default-footer
              :items-per-page="-1"
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
                <v-btn v-if="canModify && !item.concept" small @click="pick(item, 'concept')">Assign</v-btn>
                <v-btn
                  v-if="canModify && item.concept"
                  :title="item.concept.uri"
                  color="secondary"
                  small
                  @click="pick(item, 'concept')">
                  {{ item.concept.name }}
                </v-btn>
                <a v-if="!canModify && item.concept" :href="item.concept.uri" target="_blank">
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
                <a v-if="!canModify && item.concept" :href="item.unit.uri" target="_blank">
                  {{ item.unit.name }}
                </a>
              </template>
            </v-data-table>
          </v-row>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
    <v-dialog
      v-model="dialogSemantic"
      persistent
      max-width="600px">
      <DialogsSemantics
        :column="column"
        :mode="mode"
        :table-id="tableDetails.id"
        :database-id="database.id"
        @close="closed" />
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
import { formatTimestampUTCLabel, formatUser } from '@/utils'
import { decodeJwt } from 'jose'

export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      loadingConsumers: false,
      error: false,
      tables: [],
      panel: null,
      column: null,
      dialogSemantic: false,
      mode: 'unit',
      consumers: [],
      access: {
        type: null
      },
      user: {
        username: null
      },
      database: {
        id: null,
        exchange_name: null,
        is_public: null,
        tables: [],
        creator: {
          username: null
        }
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
    brokerConfig () {
      return {
        headers: { Authorization: 'Basic ' + btoa(`${this.$config.brokerUsername}:${this.$config.brokerPassword}`) },
        progress: false
      }
    },
    justCreated () {
      return new Date().getTime() - new Date(this.tableDetails.created).getTime() <= 60000
    },
    createdUTC () {
      if (this.tableDetails.created === undefined || this.tableDetails.created === null) {
        return null
      }
      return formatTimestampUTCLabel(this.tableDetails.created)
    },
    hasReadAccess () {
      if (this.database.is_public) {
        /* database is public */
        return true
      }
      if (this.database.creator.username === this.user.username) {
        /* user is creator of database */
        return true
      }
      if (this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all') {
        /* user has some level of access */
        return true
      }
      return false
    },
    consumersState () {
      if (this.consumersTotal === 0) {
        return { color: 'error', text: 'down' }
      }
      if (this.consumersTotal - this.consumersUp > 0) {
        return { color: 'warning', text: 'up' }
      }
      return { color: 'success', text: 'up' }
    },
    consumersTotal () {
      return this.consumers.length
    },
    consumersUp () {
      return this.consumers.filter(c => c.active).length
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
    this.$root.$on('table-create', this.refresh)
    this.loadUser()
    this.loadAccess()
    this.loadDatabase()
    this.pollConsumerStatus()
  },
  methods: {
    formatCreator (creator) {
      return formatUser(creator)
    },
    pick (item, mode) {
      this.column = item
      this.mode = mode
      this.dialogSemantic = true
    },
    loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
    },
    hasUnit (item) {
      return item.unit !== null
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
        this.$toast.error('Could not get database details')
      }
      this.loading = false
    },
    async loadAccess () {
      if (!this.token) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access`, this.config)
        this.access = res.data
        console.debug('access', this.access)
      } catch (err) {
        const { status } = err.response
        if (status !== 401 && status !== 403) {
          this.error = true
          this.$toast.error('Could not get database access permissions')
        }
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
    async details (table) {
      if (table.id === this.tableDetails.id) {
        /* prevent weird glitch of opening and collapsing simultaneously */
        return
      }
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
      return table.creator.username === this.user.username
    },
    closed (data) {
      console.debug('closed dialog', data)
      this.dialogSemantic = false
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
        this.$toast.error('Could not load tables')
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
