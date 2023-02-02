<template>
  <div>
    <TableToolbar :table="table" :selection="selection" />
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
    <v-card>
      <v-data-table
        :headers="headers"
        :items="rows"
        :loading="loadingData"
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
import { formatTimestampUTCLabel, formatDateUTC, formatTimestamp } from '@/utils'

export default {
  components: {
    TimeTravel,
    TableToolbar
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
      downloadLoading: false,
      dateMenu: false,
      timeMenu: false,
      selection: [],
      table: {
        name: null
      },
      pickVersionDialog: null,
      version: null,
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
    canEdit () {
      if (this.selection.length !== 1) { return false }
      return this.edit === true && this.canModify
    },
    canAdd () {
      return !this.canDelete
    },
    canDelete () {
      return this.edit && this.selection.length !== 0 && this.canModify
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
      if (!this.user || !this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
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
    this.loadData()
    this.loadTable()
      .then(() => this.loadProperties())
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
      } catch (error) {
        console.error('Failed to delete rows', error)
        const { message } = error.response
        this.$toast.error('Failed to delete rows: ' + message)
        return
      }
      this.$toast.success('Deleted ' + this.selection.length + ' rows(s)')
      this.selection = []
      /* reload */
      await this.loadData()
    },
    loadProperties () {
      try {
        console.debug('table', this.table)
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
    async loadTable () {
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, this.config)
        this.table = res.data
        console.debug('table', this.table)
      } catch (error) {
        console.error('Failed to load table', error)
        const { message } = error.response
        this.$toast.error('Failed to load table: ' + message)
      }
      this.loading = false
    },
    async loadData () {
      try {
        this.loadingData = true
        let url = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data?page=${this.options.page - 1}&size=${this.options.itemsPerPage}`
        if (this.version !== null) {
          console.info('versioning active', this.version)
          url += `&timestamp=${this.versionISO}`
        }
        const res = await this.$axios.get(url, this.config)
        this.total = parseInt(res.headers['fda-count'])
        this.rows = res.data.result
        this.rows = res.data.result.map((row) => {
          for (const col in row) {
            const columnDefinition = this.dateColumns.filter(c => c.internal_name === col)
            if (columnDefinition.length > 0) {
              if (columnDefinition[0].column_type === 'date') {
                row[col] = formatDateUTC(row[col])
              } else if (columnDefinition[0].column_type === 'timestamp') {
                row[col] = formatTimestampUTCLabel(row[col])
              }
            }
          }
          return row
        })
        console.debug('rows', this.rows)
      } catch (error) {
        console.error('Failed to load data', error)
        const { message } = error.response
        this.$toast.error('Failed to load data: ' + message)
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
