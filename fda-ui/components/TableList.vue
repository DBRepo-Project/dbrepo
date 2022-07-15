<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" />
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
        <v-expansion-panel-content>
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
                      Table Creation
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ created }}<br />
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
              <v-btn :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${item.id}`">
                View Data
              </v-btn>
              <v-btn color="secondary" class="ml-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create?tid=${item.id}`">
                Create Subset
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
                <DialogsColumnUnit :column="item" :table-id="tableDetails.id" @save="details" />
              </template>
            </v-data-table>
          </v-row>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

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
import { formatTimestamp, formatTimestampUTC } from '@/utils'
export default {
  data () {
    return {
      loading: false,
      error: false,
      tables: [],
      panel: null,
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
        created: null
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
        { value: 'BOOLEAN', text: 'Boolean' },
        { value: 'NUMBER', text: 'Number' },
        { value: 'BLOB', text: 'Binary Large Object' },
        { value: 'DATE', text: 'Date' },
        { value: 'TIMESTAMP', text: 'Timestamp' },
        { value: 'DECIMAL', text: 'Decimal' },
        { value: 'STRING', text: 'Character Varying' },
        { value: 'TEXT', text: 'Text' }
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
    created () {
      return formatTimestamp(this.tableDetails.created)
    },
    createdUTC () {
      return formatTimestampUTC(this.tableDetails.created)
    }
  },
  mounted () {
    console.debug('mounted', this.$store.state.table)
    this.$root.$on('table-create', this.refresh)
    this.loadDatabase()
  },
  methods: {
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
    details (tableId) {
      const select = this.tables.filter(t => t.id === tableId)
      if (select.length > 0) {
        this.tableDetails = select[0]
        console.debug('table details', this.tableDetails)
      }
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
</style>
