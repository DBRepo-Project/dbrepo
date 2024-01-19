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
        <v-btn v-if="user && !isExecuted" :disabled="!canExecute || !valid" :loading="loadingQuery" color="primary" @click="execute">
          <v-icon left>mdi-run</v-icon>
          Create
        </v-btn>
        <v-btn v-if="isExecuted" color="blue-grey white--text" :to="viewLink">
          <v-icon left>mdi-run</v-icon>
          View
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
        <v-card-text v-if="isView">
          <v-row dense>
            <v-col cols="6">
              <v-text-field
                v-model="view.name"
                :disabled="isExecuted"
                type="text"
                label="View name"
                :rules="[v => !!v || $t('Required'),
                         v => !validViewName(v) || $t('View name already exists')]"
                required />
            </v-col>
          </v-row>
          <v-row v-if="!view.is_public" dense>
            <v-col>
              <v-alert
                border="left"
                color="warning">
                The view metadata (name, query, etc.) will still be public, but the data will only be visible to you.
              </v-alert>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-select
                v-model="view.is_public"
                :items="visibilityOptions"
                item-text="name"
                item-value="value"
                label="View visibility" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-tabs-items v-model="tabs">
            <v-tab-item>
              <v-row dense>
                <v-col cols="6">
                  <v-select
                    v-model="table"
                    :disabled="isExecuted || loadingTables"
                    :items="tables"
                    item-text="name"
                    :loading="loadingTables"
                    return-object
                    label="Table *"
                    :rules="[v => !!v || $t('Required')]" />
                </v-col>
                <v-col cols="6">
                  <v-select
                    v-model="select"
                    item-text="name"
                    :disabled="!table || isExecuted || loadingTables"
                    :items="columns"
                    :loading="loadingColumns"
                    label="Columns *"
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
              <v-row v-if="!isView" dense>
                <v-col>
                  <v-switch
                    v-model="executeDifferentTimestamp"
                    class="ml-3"
                    color="primary"
                    :label="`Execute ${executeDifferentTimestamp ? 'on specific timestamp' : 'on latest data'}`" />
                </v-col>
              </v-row>
              <v-row v-if="!isView && executeDifferentTimestamp" dense>
                <v-col cols="6">
                  <v-text-field
                    v-model="timestamp"
                    clearable
                    :disabled="!executeDifferentTimestamp"
                    hint="YYYY-MM-dd HH:mm:ss"
                    label="Timestamp" />
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
                  <v-alert
                    border="left"
                    color="info">
                    The star selector, comments and <a href="https://mariadb.com/kb/en/aggregate-functions/" target="_blank">aggregation functions</a>
                    <sup>
                      <v-icon dense x-small>mdi-open-in-new</v-icon>
                    </sup>
                    are not supported!
                  </v-alert>
                </v-col>
              </v-row>
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
      </v-card>
    </v-form>
    <QueryResults ref="queryResults" :result-id="resultId" :type="mode" />
  </div>
</template>

<script>
import DatabaseService from '@/api/database.service'
import MiddlewareService from '@/api/middleware.service'
import TableMapper from '@/api/table.mapper'

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
      views: [],
      timestamp: null,
      executeDifferentTimestamp: false,
      foundForbiddenKeywords: [],
      forbiddenKeywords: [
        '\\*',
        'AVG',
        'BIT_AND',
        'BIT_OR',
        'BIT_XOR',
        'COUNT',
        'COUNT', 'DISTINCT',
        'GROUP_CONCAT',
        'JSON_ARRAYAGG',
        'JSON_OBJECTAGG',
        'MAX',
        'MIN',
        'STD',
        'STDDEV',
        'STDDEV_POP',
        'STDDEV_SAMP',
        'SUM',
        'VARIANCE',
        'VAR_POP',
        'VAR_SAMP',
        '--'
      ],
      tableDetails: null,
      resultId: null,
      valid: false,
      errorKeyword: null,
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
      tabs: 0,
      visibilityOptions: [
        { name: 'Public', value: true },
        { name: 'Private', value: false }
      ]
    }
  },
  computed: {
    columnNames () {
      return this.columns && this.columns.map(s => s.internal_name)
    },
    columns () {
      if (!this.table) {
        return []
      }
      return this.table.columns
    },
    tables () {
      if (!this.database) {
        return []
      }
      return this.database.tables
    },
    database () {
      return this.$store.state.database
    },
    user () {
      return this.$store.state.user
    },
    viewNames () {
      if (!this.database) {
        return []
      }
      return this.database.views.map(v => v.internal_name)
    },
    viewLink () {
      return `/database/${this.$route.params.database_id}` + (this.isView ? '/view' : '/query') + `/${this.resultId}`
    },
    sql () {
      if (this.tabs === 0) {
        return this.query.sql
      } else if (this.tabs === 1) {
        return this.rawSQL.replaceAll('\n', ' ') /* remove newline */
          .replaceAll(/\s+/g, ' ') /* remove whitespace */
          .trim()
      }
      return null
    },
    canExecute () {
      return !(!this.sql || this.sql.length === 0)
    },
    backTo () {
      return `/database/${this.$route.params.database_id}/` + (this.isView ? 'view' : 'query')
    },
    isView () {
      return this.mode === 'view'
    },
    title () {
      return this.isView ? 'Create View' : 'Create Subset'
    },
    isExecuted () {
      return this.resultId !== null
    }
  },
  watch: {
    clauses: {
      deep: true,
      handler () {
        this.buildQuery()
      }
    },
    table () {
      this.select = []
    }
  },
  mounted () {
    this.selectTable()
  },
  methods: {
    validViewName (name) {
      if (!name) {
        return false
      }
      return this.viewNames.includes(TableMapper.tableNameToInternalName(name))
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
        await this.createView()
        return
      }
      if (this.timestamp === '') {
        this.timestamp = null
      }
      await this.$refs.queryResults.executeFirstTime(this, this.sql, this.timestamp)
    },
    createView () {
      this.loadingQuery = true
      this.view.query = this.sql
      DatabaseService.createView(this.$route.params.database_id, this.view)
        .then(async (view) => {
          this.resultId = view.id
          await this.$store.dispatch('reloadDatabase')
          await Promise.all([this.$refs.queryResults.reExecute(this.resultId), this.$refs.queryResults.reExecuteCount(this.resultId)])
        })
        .finally(() => {
          this.loadingQuery = false
        })
    },
    buildQuery () {
      if (!this.table) {
        return
      }
      const data = {
        table: this.table.internal_name,
        select: this.select.map(s => s.internal_name),
        clauses: this.clauses
      }
      this.loadingQuery = true
      MiddlewareService.buildQuery(data)
        .then((query) => {
          this.query = query
        })
        .finally(() => {
          this.loadingQuery = false
        })
    }
  }
}
</script>
<style>
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
.v-data-table {
  border-radius: 0;
}
</style>
