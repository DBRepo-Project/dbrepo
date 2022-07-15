<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        {{ table.name }}
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn color="primary" :disabled="!token" class="mr-2" @click="addTuple">
          <v-icon left>mdi-plus</v-icon> Add
        </v-btn>
        <v-btn v-if="canEdit" :disabled="!token" color="warn" class="mr-2" @click="editTupleDialog = true">
          <v-icon left>mdi-pencil</v-icon> Edit
        </v-btn>
        <v-btn v-if="canDelete" :disabled="!token" color="error" class="mr-2" @click="deleteItems">
          <v-icon left>mdi-delete</v-icon> Delete<span v-if="selection.length > 1">&nbsp;{{ selection.length }}</span>
        </v-btn>
        <v-btn :disabled="!token" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create?tid=${$route.params.table_id}`" color="secondary" class="mr-2" @click="deleteItems">
          <v-icon left>mdi-wrench</v-icon> Create Subset
        </v-btn>
        <v-btn :disabled="!token" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/import`">
          <v-icon left>mdi-cloud-upload</v-icon> Import csv
        </v-btn>
        <v-btn v-if="false" color="primary" :disabled="!token" :href="`/api/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/data/export`" target="_blank">
          <v-icon left>mdi-download</v-icon> Download
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-toolbar :color="versionColor" flat>
      <v-toolbar-title>
        <strong>Versioning</strong>
        <span v-if="version !== null">{{ versionFormatted }}</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn @click="pick()">
          <v-icon left>mdi-update</v-icon> Pick
        </v-btn>
        <v-dialog
          v-model="pickVersionDialog"
          max-width="640">
          <TimeTravel ref="timeTravel" @close="pickVersionDialog = false" />
        </v-dialog>
      </v-toolbar-title>
    </v-toolbar>
    <v-card>
      <v-data-table
        :headers="headers"
        :items="rows"
        :loading="loadingData"
        :options.sync="options"
        :server-items-length="total"
        :footer-props="footerProps">
        <template v-if="token" v-slot:item.selection="{ item }">
          <input v-model="selection" type="checkbox" :value="item" @click="edit = true">
        </template>
      </v-data-table>
      <v-dialog
        v-model="editTupleDialog"
        persistent
        max-width="640">
        <EditTuple :tuple="selection[0]" :edit="edit" @close="editTupleDialog = false" />
      </v-dialog>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import EditTuple from '@/components/dialogs/EditTuple'
import TimeTravel from '@/components/dialogs/TimeTravel'
import { formatTimestampUTC } from '@/utils'

export default {
  components: {
    TimeTravel,
    EditTuple
  },
  data () {
    return {
      loading: true,
      loadingData: true,
      editTupleDialog: false,
      total: 0,
      footerProps: {
        'items-per-page-options': [10, 20, 30, 40, 50]
      },
      dateMenu: false,
      timeMenu: false,
      selection: [],
      pickVersionDialog: null,
      version: null,
      edit: false,
      error: false, // XXX: `error` is never changed
      options: {
        page: 1,
        itemsPerPage: 10
      },
      dateColumns: [],
      table: {
        name: null,
        description: null,
        columns: []
      },
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`, activeClass: '' },
        { text: 'Tables', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, activeClass: '' },
        { text: `${this.$route.params.table_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, activeClass: '' }
      ],
      headers: [],
      rows: []
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
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
      return this.formatDate(this.version)
    },
    canEdit () {
      if (this.selection.length !== 1) { return false }
      return this.edit === true
    },
    canDelete () {
      return this.selection.length !== 0
    }
  },
  watch: {
    version (newVersion, oldVersion) {
      console.info('selected new version', newVersion)
      this.loadData()
    },
    options () {
      this.loadData()
    }
  },
  mounted () {
    this.loadProperties()
    this.loadData()
  },
  methods: {
    addTuple () {
      this.edit = false
      const data = {}
      this.table.columns.forEach((c) => {
        data[c.internal_name] = null
      })
      this.selection = [data]
      this.editTupleDialog = true
    },
    pick () {
      this.pickVersionDialog = true
    },
    async deleteItems () {
      if (this.selection.length < 1) {
        return
      }
      try {
        for (const select of this.selection) {
          /* remove in container */
          const constraints = {}
          this.table.columns
            .filter(c => c.is_primary_key)
            .forEach((c) => {
              constraints[c.internal_name] = select[c.internal_name]
            })
          const res = await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data`, {
            headers: { Authorization: `Bearer ${this.token}` },
            data: { keys: constraints }
          })
          console.debug('tuple delete result', res)
        }
      } catch (err) {
        console.error('Failed to delete rows', err)
        this.$toast.error('Failed to delete rows.')
        return
      }
      this.$toast.success('Deleted ' + this.selection.length + ' rows(s)')
      this.selection = []
      /* reload */
      await this.loadData()
    },
    async loadProperties () {
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, this.config)
        this.table = res.data
        console.debug('headers', res.data.columns, 'table', this.table)
        this.headers = [{ value: 'selection', text: '', sortable: false }]
        res.data.columns.map((c) => {
          return {
            value: c.internal_name,
            text: c.name,
            sortable: false
          }
        }).forEach(header => this.headers.push(header))
        this.dateColumns = this.table.columns.filter(c => (c.column_type === 'DATE' || c.column_type === 'TIMESTAMP'))
        console.debug('date columns are', this.dateColumns)
      } catch (err) {
        this.$toast.error('Could not get table details.')
      }
      this.loading = false
    },
    async loadData () {
      try {
        this.loadingData = true
        let url = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data?page=${this.options.page - 1}&size=${this.options.itemsPerPage}`
        if (this.version !== null) {
          console.info('versioning active', this.version)
          url += `&timestamp=${new Date(this.version).toISOString()}`
        }
        const res = await this.$axios.get(url, this.config)
        this.total = parseInt(res.headers['fda-count'])
        this.rows = res.data.result.map((row) => {
          for (const col in row) {
            if (this.dateColumns.filter(c => c.internal_name === col).length > 0) {
              row[col] = formatTimestampUTC(row[col])
            }
          }
          return row
        })
        console.debug('rows', this.rows)
      } catch (err) {
        console.error('failed to load data', err)
        this.$toast.error('Could not load table data.')
      }
      this.loadingData = false
    }
  }
}
</script>

<style>
thead td:first-child,
tbody td:first-child {
  width: 1%;
}
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
</style>
