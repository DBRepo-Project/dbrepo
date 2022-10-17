<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="backTo">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>{{ title }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn :disabled="!canExecute || !token || !valid || isExecuted" :loading="loadingQuery" color="primary" @click="execute">
          <v-icon left>mdi-run</v-icon>
          Create
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
    <v-form v-model="valid">
      <v-card flat>
        <v-card-text>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-if="isView"
                v-model="view.name"
                :disabled="isExecuted"
                type="text"
                label="View name"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-tabs-items v-model="tabs">
            <v-tab-item>
              <v-row>
                <v-col cols="6">
                  <v-select
                    v-model="table"
                    :disabled="isExecuted"
                    :items="tables"
                    item-text="name"
                    :loading="loadingTables"
                    return-object
                    label="Table"
                    :rules="[v => !!v || $t('Required')]"
                    @change="loadColumns" />
                </v-col>
                <v-col cols="6">
                  <v-select
                    v-model="select"
                    item-text="name"
                    :disabled="!table || isExecuted"
                    :items="selectItems"
                    :loading="loadingColumns"
                    label="Columns"
                    :rules="[v => !!v || $t('Required')]"
                    return-object
                    multiple
                    @change="buildQuery" />
                </v-col>
              </v-row>
              <QueryFilters
                v-if="table"
                v-model="clauses"
                :disabled="isExecuted"
                :columns="columnNames" />
              <v-row>
                <v-col>
                  <v-switch
                    v-if="isView"
                    v-model="view.is_public"
                    :label="`${view.is_public ? 'Public' : 'Private'} view`" />
                </v-col>
              </v-row>
              <v-row v-if="query.formatted" id="query-raw">
                <v-col>
                  <span class="subtitle-1">Generated SQL-Query:</span>
                  <QueryRaw
                    v-model="query.formatted"
                    disabled
                    class="mt-2 ml-3" />
                </v-col>
              </v-row>
            </v-tab-item>
            <v-tab-item>
              <v-row>
                <v-col>
                  <QueryRaw
                    v-model="rawSQL"
                    class="mt-2 ml-3" />
                </v-col>
              </v-row>
            </v-tab-item>
          </v-tabs-items>
        </v-card-text>
        <v-card-text v-if="isExecuted">
          <v-row>
            <v-col>
              <v-btn color="blue-grey white--text" :to="viewLink">
                View
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-form>
    <QueryResults ref="queryResults" v-model="queryId" />
  </div>
</template>

<script>
export default {
  props: {
    mode: {
      type: String,
      default () {
        return 'query'
      }
    }
  },
  data () {
    return {
      table: {},
      tables: [],
      tableDetails: null,
      queryId: null,
      viewId: null,
      valid: false,
      query: {
        sql: ''
      },
      view: {
        is_public: true,
        name: null,
        query: null
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
    tableId () {
      return this.table.id
    },
    viewLink () {
      return `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}` + (this.isView ? `/view/${this.viewId}` : `/query/${this.queryId}`)
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
    },
    backTo () {
      return `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/` + (this.isView ? 'view' : 'query')
    },
    isView () {
      return this.mode === 'view'
    },
    title () {
      return this.isView ? 'Create View' : 'Create Subset'
    },
    isExecuted () {
      return this.viewId !== null || this.queryId !== null
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
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, this.config)
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
    async execute () {
      if (this.isView) {
        this.loadingQuery = true
        try {
          this.view.query = this.query.sql
          const res = await this.$axios.post(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view`, this.view, this.config)
          console.debug('view', res.data)
          this.viewId = res.data.id
        } catch (err) {
          console.error('Failed to create view', err)
          return
        }
        this.loadingQuery = false
      }
      await this.$refs.queryResults.executeFirstTime(this)
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
      if (!this.tableId) {
        return
      }
      try {
        this.loadingColumns = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.tableId}`, this.config)
        this.tableDetails = res.data
        this.buildQuery()
      } catch (err) {
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
#query-raw {
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
