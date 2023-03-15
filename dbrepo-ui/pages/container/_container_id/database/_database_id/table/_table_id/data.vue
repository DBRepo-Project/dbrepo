<template>
  <div v-if="canRead">
    <TableToolbar :selection="selection" @modified="modified" />
    <v-toolbar :color="versionColor" flat>
      <v-toolbar-title>
        <strong>Versioning</strong>
        <span v-if="version !== null">{{ versionFormatted }}</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn class="mr-2" :loading="downloadLoading" @click.stop="download">
          <v-icon left>mdi-download</v-icon> Download csv
        </v-btn>
        <v-btn @click="pick">
          <v-icon left>mdi-update</v-icon> Pick
        </v-btn>
        <v-dialog
          v-model="pickVersionDialog"
          max-width="640"
          @close="closeVersion">
          <TimeTravel ref="timeTravel" @close="pickVersion" />
        </v-dialog>
      </v-toolbar-title>
    </v-toolbar>
    <v-card tile>
      <v-progress-linear v-if="loadingData > 0 || error" :value="loadProgress" :color="error ? 'error' : 'primary'" />
      <v-data-table
        :headers="headers"
        :items="rows"
        :options.sync="options"
        :server-items-length="total"
        :footer-props="footerProps">
        <template v-if="canModify" v-slot:item.selection="{ item }">
          <input v-model="selection" type="checkbox" :value="item" @click="edit = true">
        </template>
      </v-data-table>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TimeTravel from '@/components/dialogs/TimeTravel'
import TableToolbar from '@/components/TableToolbar'
import { formatTimestampUTC, formatDateUTC, formatTimestamp } from '@/utils'

export default {
  components: {
    TimeTravel,
    TableToolbar
  },
  data () {
    return {
      loading: true,
      loadingData: 0,
      loadProgress: 0,
      editTupleDialog: false,
      total: -1,
      footerProps: {
        'items-per-page-options': [10, 20, 30, 40, 50]
      },
      downloadLoading: false,
      dateMenu: false,
      timeMenu: false,
      selection: [],
      pickVersionDialog: null,
      version: null,
      lastReload: new Date(),
      tab: null,
      error: false, // XXX: `error` is never changed
      options: {
        page: 1,
        itemsPerPage: 10
      },
      dateColumns: [],
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
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
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
    tables () {
      return this.$store.state.tables
    },
    access () {
      return this.$store.state.access
    },
    downloadConfig () {
      if (this.token === null) {
        return {
          responseType: 'text'
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` },
        responseType: 'text'
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
      return this.version + ' (UTC)'
    },
    versionISO () {
      if (this.version === null) {
        return null
      }
      return this.version.substring(0, 10) + 'T' + this.version.substring(11, 19) + 'Z'
    },
    canModify () {
      if (!this.user || !this.access || !this.table || !this.table.creator) {
        return false
      }
      if (this.table.creator.username === this.user.username) {
        return true
      }
      if (this.access.type === 'write_own' && this.table.creator.username === this.user.username) {
        return true
      }
      return this.access.type === 'write_all'
    },
    canRead () {
      if (this.database?.is_public) {
        return true
      }
      if (!this.user || !this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
    }
  },
  watch: {
    version (newVersion, oldVersion) {
      console.info('selected new version', newVersion)
      this.reload()
    },
    options () {
      this.loadData()
    },
    table (newTable, oldTable) {
      if (newTable !== oldTable && oldTable === null) {
        this.loadProperties()
      }
    }
  },
  mounted () {
    this.reload()
    this.simulateProgress()
    this.loadProperties()
  },
  methods: {
    async download () {
      this.downloadLoading = true
      try {
        let exportUrl = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/export`
        if (this.version) {
          exportUrl += `?timestamp=${this.versionISO}`
        }
        const res = await this.$axios.get(exportUrl, this.downloadConfig)
        console.debug('export table', res)
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'table.csv')
        document.body.appendChild(link)
        link.click()
      } catch (error) {
        console.error('Failed to export table', error)
        const { message } = error.response
        this.$toast.error('Failed to export table: ' + message)
        this.error = true
      }
      this.downloadLoading = false
    },
    pick () {
      if (this.$refs.timeTravel !== undefined) {
        /* when the component was loaded once, this method refreshes the content */
        this.$refs.timeTravel.loadHistory()
      }
      this.pickVersionDialog = true
    },
    closeVersion () {
      this.pickVersionDialog = false
    },
    pickVersion (event) {
      const date = new Date(event.time)
      date.setSeconds(date.getSeconds() + 1)
      console.debug('closed', event)
      if (event.time) {
        this.version = formatTimestamp(date)
      }
      this.pickVersionDialog = false
    },
    loadProperties () {
      if (!this.table || this.headers.length > 0) {
        return
      }
      try {
        this.headers = [{ value: 'selection', text: '', sortable: false }]
        this.table.columns.map((c) => {
          return {
            value: c.internal_name,
            text: c.name,
            sortable: false
          }
        }).forEach(header => this.headers.push(header))
        this.dateColumns = this.table.columns.filter(c => (c.column_type === 'date' || c.column_type === 'timestamp'))
        console.debug('date columns are', this.dateColumns)
      } catch (error) {
        console.error('Failed to map table details', error)
        const { message } = error.response
        this.$toast.error('Failed to map table details: ' + message)
      }
      this.loading = false
    },
    modified (event) {
      const { success, action } = event
      if (action === 'add') {
        this.selection = [event.data]
      } else {
        this.selection = []
      }
      if (success) {
        this.reload()
      }
    },
    reload () {
      this.lastReload = new Date()
      this.loadData()
      this.loadCount()
    },
    async loadData () {
      try {
        this.loadingData++
        const url = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data?page=${this.options.page - 1}&size=${this.options.itemsPerPage}&timestamp=${this.versionISO || this.lastReload.toISOString()}`
        if (this.version !== null) {
          console.info('versioning active', this.version)
        }
        const res = await this.$axios.get(url, this.config)
        this.rows = res.data.result.map((row) => {
          for (const col in row) {
            const columnDefinition = this.dateColumns.filter(c => c.internal_name === col)
            if (columnDefinition.length > 0) {
              if (columnDefinition[0].column_type === 'date') {
                row[col] = formatDateUTC(row[col])
              } else if (columnDefinition[0].column_type === 'timestamp') {
                row[col] = formatTimestampUTC(row[col])
              }
            }
          }
          return row
        })
        console.debug('rows', this.rows)
      } catch (error) {
        console.error('Failed to load data', error)
        this.error = true
        this.loadProgress = 100
        const { status, data } = error.response
        const { message, code } = data
        if (status === 423) {
          console.error('Database is offline', code)
          this.$toast.error('Database is offline: ' + message)
        } else {
          console.error('Failed to load data', code)
          this.$toast.error('Failed to load data: ' + message)
        }
      } finally {
        this.loadingData--
      }
    },
    async loadCount () {
      try {
        this.loadingData++
        const url = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data/count?timestamp=${this.versionISO || this.lastReload.toISOString()}`
        if (this.version !== null) {
          console.info('versioning active', this.version)
        }
        const res = await this.$axios.get(url, this.config)
        this.total = res.data
        console.info('total', this.total)
      } catch (error) {
        console.error('Failed to load count', error)
        this.error = true
        this.loadProgress = 100
        const { status, data } = error.response
        const { message, code } = data
        if (status === 423) {
          console.error('Database is offline', code)
          this.$toast.error('Database is offline: ' + message)
        } else {
          console.error('Failed to load data', code)
          this.$toast.error('Failed to load data: ' + message)
        }
      } finally {
        this.loadingData--
      }
    },
    simulateProgress () {
      if (this.loadProgress !== 0) {
        return
      }
      const timeout = 30 * 1000 /* ms */
      const ticks = 100 /* ms */
      let i = 0
      setInterval(() => {
        if (i++ >= timeout && !this.error) {
          return
        }
        this.loadProgress = ((i * 100) / timeout) * 100
      }, ticks)
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
