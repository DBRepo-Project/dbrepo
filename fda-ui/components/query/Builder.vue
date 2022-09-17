<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>Create Subset</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn :disabled="!canExecute || !token" color="primary" @click="execute">
          <v-icon left>mdi-run</v-icon>
          Execute
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-toolbar flat>
      <v-tabs
        v-model="tabs"
        color="primary">
        <v-tab>
          Simple
        </v-tab>
        <v-tab>
          Expert
        </v-tab>
      </v-tabs>
    </v-toolbar>
    <v-card flat>
      <v-tabs-items v-model="tabs">
        <v-tab-item>
          <v-card-text>
            <v-row>
              <v-col cols="6">
                <v-select
                  v-model="table"
                  :items="tables"
                  item-text="name"
                  :loading="loadingTables"
                  return-object
                  label="Table"
                  @change="loadColumns" />
              </v-col>
              <v-col cols="6">
                <v-select
                  v-model="select"
                  item-text="name"
                  :disabled="!table"
                  :items="selectItems"
                  :loading="loadingColumns"
                  label="Columns"
                  return-object
                  multiple
                  @change="buildQuery" />
              </v-col>
            </v-row>
            <QueryFilters
              v-if="table"
              v-model="clauses"
              :columns="columnNames" />
            <v-row v-if="query.formatted">
              <v-col>
                <v-progress-linear v-if="loadingQuery" color="primary" />
                <QueryRaw
                  v-model="query.formatted"
                  disabled
                  class="mt-2 ml-3" />
              </v-col>
            </v-row>
          </v-card-text>
        </v-tab-item>
        <v-tab-item>
          <QueryRaw
            v-model="rawSQL"
            class="mt-2 ml-3" />
        </v-tab-item>
      </v-tabs-items>
      <v-card-text v-if="queryId">
        <v-row>
          <v-col>
            <v-btn color="blue-grey white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/${queryId}`">
              View
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
    <QueryResults ref="queryResults" v-model="queryId" />
  </div>
</template>

<script>
export default {
  data () {
    return {
      table: null,
      tables: [],
      tableDetails: null,
      queryId: null,
      query: {
        sql: ''
      },
      loadingTables: false,
      loadingColumns: false,
      loadingQuery: false,
      rawSQL: '',
      select: [],
      clauses: [],
      tabs: 0
    }
  },
  computed: {
    selectItems () {
      const columns = this.tableDetails && this.tableDetails.columns
      return columns || []
    },
    columnNames () {
      return this.selectItems && this.selectItems.map(s => s.internal_name)
    },
    token () {
      return this.$store.state.token
    },
    headers () {
      if (this.token === null) {
        return null
      }
      return { Authorization: `Bearer ${this.token}` }
    },
    sql () {
      if (this.tabs === 0) {
        // builder
        return this.query.sql
      } else {
        // raw sql
        return this.rawSQL
      }
    },
    canExecute () {
      if (this.tabs === 0) {
        // builder
        return this.sql.length &&
                 this.select.length // select `*` columns not supported in backend
      } else {
        // raw sql
        return this.sql.length
      }
    }
  },
  watch: {
    clauses: {
      deep: true,
      handler () {
        this.buildQuery()
        this.queryId = null
      }
    },
    table () {
      this.queryId = null
    },
    sql () {
      this.queryId = null
    },
    select () {
      this.queryId = null
    }
  },
  mounted () {
    this.loadTables()
      .then(() => this.selectTable())
      .then(() => this.loadColumns())
  },
  methods: {
    async loadTables () {
      try {
        this.loadingTables = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, {
          headers: this.headers
        })
        this.tables = res.data
        console.debug('tables', this.tables)
      } catch (err) {
        this.$toast.error('Could not list table.')
      }
      this.loadingTables = false
    },
    selectTable () {
      if (this.$route.query.tid === undefined) {
        return
      }
      const tid = parseInt(this.$route.query.tid)
      const selection = this.tables.filter(t => t.id === tid)
      if (selection.length > 0) {
        this.table = selection[0]
        console.info('Preselect table with id', tid)
        console.debug('preselected table', this.table)
      } else {
        console.warn('Failed to find table with id', tid)
      }
    },
    execute () {
      this.$refs.queryResults.executeFirstTime(this)
    },
    async buildQuery () {
      if (!this.table) {
        return
      }
      const url = '/server-middleware/query/build'
      const data = {
        table: this.table.internal_name,
        select: this.select.map(s => s.internal_name),
        clauses: this.clauses
      }
      try {
        this.loadingQuery = true
        const res = await this.$axios.post(url, data)
        if (res && !res.error) {
          this.query = res.data
        }
      } catch (e) {
        console.log(e)
      }
      this.loadingQuery = false
    },
    async loadColumns () {
      if (this.table === null) {
        return
      }
      try {
        this.loadingColumns = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.table.id}`, {
          headers: this.headers
        })
        this.tableDetails = res.data
        this.buildQuery()
      } catch (err) {
        console.error('Could not get table details', err)
        this.$toast.error('Could not get table details.')
      }
      this.loadingColumns = false
    }
  }
}
</script>

<style lang="scss" scoped>
/* these are taked from solarized-light (plugins/vendors.js), to override the
main.scss file from vuetify, because it paints it red */
::v-deep code {
  background: #fdf6e3;
  color: #657b83;
}

</style>
