<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
    <v-toolbar flat>
      <v-toolbar-title>
        {{ table.name }}
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon> Back
        </v-btn>
        <v-btn v-if="selection.length === 1" color="amber darken-2" class="mr-2 white--text">
          <v-icon left>mdi-pencil</v-icon> Edit
        </v-btn>
        <v-btn v-if="selection.length > 0" color="red darken-2" class="white--text" @click="deleteItems">
          <v-icon left>mdi-delete</v-icon> Delete<span v-if="selection.length > 1">&nbsp;{{ selection.length }}</span>
        </v-btn>
        <v-btn v-if="selection.length === 0" :disabled="!token" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/import`">
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
        <v-btn @click="pickVersionDialog = true">
          <v-icon left>mdi-update</v-icon> Pick
        </v-btn>
        <v-dialog
          v-model="pickVersionDialog"
          max-width="640">
          <TimeTravel @close="pickVersionDialog = false" />
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
        :footer-props="footerProps"
        class="elevation-1">
        <template v-slot:item.selection="{ item }">
          <input type="checkbox" :value="item" v-model="selection">
        </template>
      </v-data-table>
    </v-card>
    <div class="mt-3">
      <v-chip
        class="mr-2"
        label>
        ‡ Primary Key
      </v-chip>
      <v-chip
        class="mr-2"
        label>
        † Unique Column
      </v-chip>
    </div>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TimeTravel from '@/components/dialogs/TimeTravel'
import { format } from 'date-fns'

export default {
  components: {
    TimeTravel
  },
  data () {
    return {
      loading: true,
      loadingData: true,
      total: 0,
      footerProps: {
        'items-per-page-options': [10, 20, 30, 40, 50]
      },
      dateMenu: false,
      timeMenu: false,
      selection: [],
      pickVersionDialog: null,
      version: null,
      error: false, // XXX: `error` is never changed
      options: {
        page: 1,
        itemsPerPage: 10
      },
      table: {
        name: null,
        description: null
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
    requestHeaders () {
      if (this.token === null) {
        return null
      }
      return { Authorization: `Bearer ${this.token}` }
    },
    versionColor () {
      if (this.version === null) {
        return 'grey lighten-1'
      }
      return 'primary white--text'
    },
    versionFormatted () {
      if (this.version === null) {
        return null
      }
      return this.formatDate(this.version)
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
            headers: this.requestHeaders,
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
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`)
        this.table = res.data
        console.debug('headers', res.data.columns, 'table', this.table)
        this.headers = [{ value: 'selection', text: '', sortable: false }]
        res.data.columns.map((c) => {
          return {
            value: c.internal_name,
            text: this.columnAddition(c) + c.name,
            sortable: false
          }
        }).forEach(header => this.headers.push(header))
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
        const res = await this.$axios.get(url)
        this.total = parseInt(res.headers['fda-count'])
        this.rows = res.data.result
        console.debug('rows', this.rows)
      } catch (err) {
        console.error('failed to load data', err)
        this.$toast.error('Could not load table data.')
      }
      this.loadingData = false
    },
    columnAddition (column) {
      if (column.is_primary_key) {
        return '‡ '
      }
      if (column.unique) {
        return '† '
      }
      return ''
    },
    formatDate (d) {
      return format(new Date(d), 'dd.MM.yyyy HH:mm')
    }
  }
}
</script>

<style>
thead td:first-child,
tbody td:first-child {
  width: 1%;
}
</style>
