<template>
  <div v-if="canViewTableData">
    <TableToolbar :selection="selection" @modified="modified" />
    <v-toolbar :color="versionColor" flat>
      <v-toolbar-title>
        <strong>Current</strong>
        <span v-if="version !== null">{{ versionFormatted }}</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn :loading="downloadLoading" @click.stop="download">
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
      <v-progress-linear v-if="loadingData > 0 || error" :indeterminate="!error" :color="loadingColor" />
      <v-card v-if="error" flat tile>
        <v-card-text>
          Failed to load table data: database is not reachable
        </v-card-text>
      </v-card>
      <v-data-table
        v-if="!error"
        flat
        :headers="headers"
        :items="rows"
        :options.sync="options"
        :server-items-length="total"
        :footer-props="footerProps">
        <template v-if="canModify" v-slot:item.selection="{ item }">
          <input v-model="selection" type="checkbox" :value="item" @click="edit = true">
        </template>
        <template v-for="(blobColumn,idx) in blobColumns" v-slot:[blobColumn]="{ item }">
          <a :key="`b-${idx}`" :href="item.blob">Download</a>
        </template>
      </v-data-table>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TimeTravel from '@/components/dialogs/TimeTravel.vue'
import TableToolbar from '@/components/table/TableToolbar.vue'
import TableService from '@/api/table.service'
import { formatTimestampUTC, formatDateUTC, formatTimestamp, formatBinaryStream } from '@/utils'

export default {
  components: {
    TimeTravel,
    TableToolbar
  },
  data () {
    return {
      loading: true,
      loadingData: 0,
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
      edit: false,
      error: false,
      options: {
        page: 1,
        itemsPerPage: 10
      },
      dateColumns: [],
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}/info`, activeClass: '' },
        { text: 'Tables', to: `/database/${this.$route.params.database_id}/table`, activeClass: '' },
        { text: `${this.$route.params.table_id}`, to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, activeClass: '' }
      ],
      headers: [],
      rows: []
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'error' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    roles () {
      return this.$store.state.roles
    },
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
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
    blobColumns () {
      if (!this.table || !this.table.columns) {
        return []
      }
      return this.table.columns.filter(c => this.isFileField(c)).map(c => 'item.' + c.internal_name)
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
      if (!this.user || !this.access || !this.table) {
        return false
      }
      if (this.access.type === 'write_own' && this.table.owner.id === this.user.id) {
        return true
      }
      return this.access.type === 'write_all'
    },
    canViewTableData () {
      /* view when database is public or when private: 1) view-table-data role present 2) access is at least read */
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (!this.roles || !this.roles.includes('view-table-data') || !this.access) {
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
    this.loadProperties()
  },
  methods: {
    download () {
      this.downloadLoading = true
      if (!this.version) {
        TableService.exportData(this.$route.params.database_id, this.$route.params.table_id)
          .then((data) => {
            const url = window.URL.createObjectURL(new Blob([data]))
            const link = document.createElement('a')
            link.href = url
            link.setAttribute('download', 'table.csv')
            document.body.appendChild(link)
            link.click()
          })
          .catch(() => {
            this.downloadLoading = false
          })
          .finally(() => {
            this.downloadLoading = false
          })
      } else {
        TableService.exportData(this.$route.params.database_id, this.$route.params.table_id, this.versionISO)
          .then((data) => {
            const url = window.URL.createObjectURL(new Blob([data]))
            const link = document.createElement('a')
            link.href = url
            link.setAttribute('download', `table_${this.versionISO}.csv`)
            document.body.appendChild(link)
            link.click()
          })
          .catch(() => {
            this.downloadLoading = false
          })
          .finally(() => {
            this.downloadLoading = false
          })
      }
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
    loadData () {
      this.loadingData++
      TableService.data(this.$route.params.database_id, this.$route.params.table_id, (this.options.page - 1), this.options.itemsPerPage, (this.versionISO || this.lastReload.toISOString()))
        .then((data) => {
          this.rows = data.result.map((row) => {
            for (const col in row) {
              const column = this.table.columns.filter(c => c.internal_name === col)[0]
              if (['blob', 'tinyblob', 'mediumblob', 'longblob'].includes(column.column_type)) {
                row[col] = formatBinaryStream(row[col])
                continue
              }
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
        })
        .catch((error) => {
          console.error('load data resulted in error', error)
          this.error = true
        })
        .finally(() => {
          this.loadingData--
        })
    },
    loadCount () {
      this.loadingData++
      TableService.dataCount(this.$route.params.database_id, this.$route.params.table_id, (this.versionISO || this.lastReload.toISOString()))
        .then((count) => {
          this.total = count
        })
        .catch(() => {
          this.loadingData--
        })
        .finally(() => {
          this.loadingData--
        })
    },
    isFileField (column) {
      return ['blob', 'longblob', 'mediumblob', 'tinyblob'].includes(column.column_type)
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
