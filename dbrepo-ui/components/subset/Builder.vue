<template>
  <div>
    <v-toolbar flat>
      <v-btn
        size="small"
        variant="plain"
        icon="mdi-arrow-left"
        :to="backTo" />
      <v-toolbar-title
        :text="title" />
      <v-spacer />
      <v-btn
        v-if="user"
        :disabled="!canExecute"
        color="secondary"
        variant="flat"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-run' : null"
        :text="$t('navigation.create')"
        @click="execute" />
    </v-toolbar>
    <v-toolbar flat>
      <v-tabs
        v-model="tabs"
        color="primary">
        <v-tab
          value="0"
          :text="$t('pages.subset.subpages.create.simple.text')" />
        <v-tab
          value="1"
          :text="$t('pages.subset.subpages.create.expert.text')" />
      </v-tabs>
    </v-toolbar>
    <TimeDrift />
    <v-card
      rounded="0"
      variant="flat">
      <v-card-text>
        <v-form
          ref="formView"
          v-model="valid"
          @submit.prevent="prevent">
          <v-row
            v-if="isView"
            class="mt-1"
            dense>
            <v-col md="8">
              <v-text-field
                v-model="view.name"
                :disabled="isExecuted"
                type="text"
                clearable
                persistent-hint
                :variant="inputVariant"
                :label="$t('pages.view.subpages.create.name.label')"
                :hint="$t('pages.view.subpages.create.name.hint')"
                :rules="[v => !!v || $t('validation.required'),
                     v => !validViewName(v) || $t('validation.view.exists')]"
                required />
            </v-col>
          </v-row>
          <v-row
            v-if="isView && !view.is_public"
            dense>
            <v-col>
              <v-alert
                :text="$t('pages.view.subpages.create.visibility.warn')"
                border="start"
                color="warning" />
            </v-col>
          </v-row>
          <v-row
            v-if="isView"
            dense>
            <v-col md="8">
              <v-select
                v-model="view.is_public"
                :items="visibilities"
                persistent-hint
                :variant="inputVariant"
                required
                clearable
                :label="$t('pages.view.subpages.create.visibility.label')"
                :hint="$t('pages.view.subpages.create.visibility.hint')"
                :rules="[v => !!v || $t('validation.required')]" />
            </v-col>
          </v-row>
          <v-window
            class="mt-4"
            v-model="tabs">
            <v-window-item
              value="0">
              <v-row dense>
                <v-col md="4">
                  <v-select
                    v-model="table"
                    :disabled="isExecuted"
                    :items="tables"
                    item-title="name"
                    return-object
                    persistent-hint
                    clearable
                    :variant="inputVariant"
                    :label="$t('pages.view.subpages.create.table.label')"
                    :hint="$t('pages.view.subpages.create.table.hint')"
                    :rules="[v => !!v || $t('validation.required')]" />
                </v-col>
                <v-col md="4">
                  <v-select
                    v-model="select"
                    item-title="internal_name"
                    :disabled="!table || isExecuted"
                    :items="columns"
                    persistent-hint
                    clearable
                    :variant="inputVariant"
                    :label="$t('pages.view.subpages.create.columns.label')"
                    :hint="$t('pages.view.subpages.create.columns.hint')"
                    :rules="[v => !!v || $t('validation.required')]"
                    return-object
                    multiple
                    @update:model-value="buildQuery" />
                </v-col>
              </v-row>
              <v-row v-if="select.length > 0">
                <v-col md="8">
                  <v-btn
                    v-if="clauses.length === 0"
                    size="small"
                    color="secondary"
                    variant="flat"
                    :text="$t('pages.subset.subpages.create.filter.text')"
                    :disabled="clausesDisabled"
                    @click="addFirst" />
                </v-col>
              </v-row>
              <div class="mb-5">
                <v-row v-if="clauses.length > 0">
                  <v-col
                    md="8"
                    class="text-center">
                    <pre>WHERE</pre>
                  </v-col>
                </v-row>
                <div v-for="(clause, idx) in clauses" :key="idx">
                  <v-row
                    v-if="clause.type === 'where'">
                    <v-col md="3">
                      <v-select
                        v-model="clause.params[0]"
                        :disabled="clausesDisabled"
                        item-title="internal_name"
                        item-value="internal_name"
                        variant="underlined"
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.column.label')"
                        :hint="$t('pages.subset.subpages.create.filter.column.hint')"
                        :items="select" />
                    </v-col>
                    <v-col md="1">
                      <v-select
                        v-model="clause.params[1]"
                        :disabled="clausesDisabled"
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.operator.label')"
                        :hint="$t('pages.subset.subpages.create.filter.operator.hint')"
                        :items="operators" />
                    </v-col>
                    <v-col md="3">
                      <v-text-field
                        v-model="clause.params[2]"
                        :disabled="clausesDisabled"
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.value.label')"
                        :hint="$t('pages.subset.subpages.create.filter.value.hint')" />
                    </v-col>
                    <v-col md="1">
                      <v-btn
                        :disabled="clausesDisabled"
                        class="mt-4"
                        size="small"
                        color="error"
                        variant="flat"
                        :text="$t('pages.subset.subpages.create.filter.remove.text')"
                        @click="remove(idx)" />
                    </v-col>
                  </v-row>
                  <v-row
                    v-else>
                    <v-col
                      md="8"
                      class="text-center">
                      <pre v-text="clause.type.toUpperCase()" />
                    </v-col>
                  </v-row>
                  <div
                    v-if="clause.params && canAdd(idx)">
                    <v-row
                      dense>
                      <v-col>
                        <v-btn
                          :disabled="!canAdd(idx) || clausesDisabled"
                          class="mt-2 mr-1"
                          variant="flat"
                          color="secondary"
                          size="small"
                          :text="$t('pages.subset.subpages.create.filter.and.text')"
                          @click="addAnd" />
                        <v-btn
                          :disabled="!canAdd(idx) || clausesDisabled"
                          class="mt-2"
                          variant="flat"
                          color="secondary"
                          size="small"
                          :text="$t('pages.subset.subpages.create.filter.or.text')"
                          @click="addOr" />
                      </v-col>
                    </v-row>
                  </div>
                </div>
              </div>
              <v-row
                dense>
                <v-col
                  v-text="$t('pages.subset.subpages.create.generated')" />
              </v-row>
              <v-row
                id="query-raw"
                dense>
                <v-col>
                  <Raw
                    :value="query.formatted"
                    disabled
                    class="mt-2" />
                </v-col>
              </v-row>
            </v-window-item>
            <v-window-item
              value="1">
              <v-row
                v-if="hasUnsupported"
                dense>
                <v-col>
                  <v-alert
                    border="start"
                    color="warning">
                    <span v-text="$t('pages.subset.subpages.create.expert.warn')" />
                    <pre style="white-space:inherit;" v-text="unsupported.join(', ')" />
                  </v-alert>
                </v-col>
              </v-row>
              <v-row dense>
                <v-col v-text="$t('pages.subset.subpages.create.subtitle')" />
              </v-row>
              <v-row dense>
                <v-col>
                  <Raw
                    class="mt-2"
                    @sql="updateSql" />
                </v-col>
              </v-row>
            </v-window-item>
          </v-window>
        </v-form>
      </v-card-text>
    </v-card>
    <Results
      ref="queryResults"
      :result-id="resultId"
      :type="mode" />
  </div>
</template>

<script>
import TimeDrift from '@/components/TimeDrift'
import Raw from '@/components/subset/Raw'
import Results from '@/components/subset/Results'
import { useCacheStore } from '@/stores/cache'
import { useUserStore } from '@/stores/user'
import { format } from 'sql-formatter'
import { localizedMessage } from '@/utils'

export default {
  components: {
    Raw,
    Results,
    TimeDrift
  },
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
      table: null,
      views: [],
      timestamp: null,
      executeDifferentTimestamp: false,
      visibilities: [
        { title: this.$t('toolbars.database.public'), value: true },
        { title: this.$t('toolbars.database.private'), value: false },
      ],
      operators: [
        '=',
        '<',
        '>',
        '<=',
        '>=',
        '<>',
        '!=',
        'like',
        'not like',
        'between',
        'not between',
        'ilike',
        'not ilike',
        'exists',
        'not exist',
        'rlike',
        'not rlike',
        'regexp',
        'not regexp',
        'match',
        '&',
        '|',
        '^',
        '<<',
        '>>',
        '~',
        '~=',
        '~*',
        '!~',
        '!~*',
        '#',
        '&&',
        '@>',
        '<@',
        '||',
        '&<',
        '&>',
        '-|-',
        '@@',
        '!!'
      ],
      tableDetails: null,
      resultId: null,
      valid: false,
      errorKeyword: null,
      query: {
        raw: null,
        formatted: null
      },
      view: {
        is_public: true,
        name: null,
        query: null
      },
      select: [],
      clauses: [],
      tabs: 0,
      cacheStore: useCacheStore(),
      userStore: useUserStore()
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
      return this.cacheStore.getDatabase
    },
    user () {
      return this.userStore.getUser
    },
    viewNames () {
      if (!this.database) {
        return []
      }
      return this.database.views.map(v => v.internal_name)
    },
    sql () {
      if (!this.query.raw) {
        return ''
      }
      return this.query.raw.replaceAll('\n', ' ') /* remove newline */
        .replaceAll(/\s+/g, ' ') /* remove whitespace */
        .trim()
    },
    clausesDisabled () {
      return this.isExecuted
    },
    backTo () {
      return `/database/${this.$route.params.database_id}/` + (this.isView ? 'view' : 'subset')
    },
    isView () {
      return this.mode === 'view'
    },
    title () {
      return this.isView ? this.$t('pages.view.subpages.create.title') : this.$t('pages.subset.subpages.create.title')
    },
    isExecuted () {
      return this.resultId !== null
    },
    valid () {
      if (this.isView) {
        return this.valid && !this.hasUnsupported
      }
      return this.sql.length > 0 && !this.hasUnsupported
    },
    unsupported () {
      if (!this.$config.public.database.unsupported) {
        return []
      }
      return this.$config.public.database.unsupported.split(',')
    },
    hasUnsupported () {
      if (!this.sql) {
        return false
      }
      const unsupported = this.unsupported.map(k => k.toLowerCase())
      for (let i = 0; i < unsupported.length; i++) {
        if (this.sql.toLowerCase().includes(unsupported[i])) {
          console.warn('query contains unsupported keyword', unsupported[i])
          return true
        }
      }
      return false
    },
    canExecute () {
      if (this.isView) {
        return this.view.name !== null && this.view.is_public !== null && this.view.query !== null
      }
      return this.valid
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  watch: {
    clauses: {
      deep: true,
      immediate: true,
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
    prevent () {
      this.$refs.formView.validate()
    },
    validViewName (name) {
      if (!name) {
        return false
      }
      const tableService = useTableService()
      return this.viewNames.includes(tableService.tableNameToInternalName(name))
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
      /* pre-check */
      const queryService = useQueryService()
      queryService.execute(this.$route.params.database_id, { statement: this.sql, timestamp: this.timestamp }, 0, 1)
        .then((subset) => {
          this.$refs.queryResults.executeFirstTime(this, this.sql, this.timestamp)
          this.$toast.success(this.$t('success.subset.create'))
          this.$router.push(`/database/${this.$route.params.database_id}/subset/${subset.id}/data`)
        })
        .catch((error) => {
          this.$toast.error(localizedMessage(this.$t, error, null))
        })
    },
    createView () {
      this.loadingQuery = true
      this.view.query = this.sql
      const viewService = useViewService()
      viewService.create(this.$route.params.database_id, this.view)
        .then((view) => {
          this.resultId = view.id
          Promise.all([this.$refs.queryResults.reExecute(this.resultId), this.$refs.queryResults.reExecuteCount(this.resultId)])
          this.cacheStore.reloadDatabase()
          this.$toast.success(this.$t('success.view.create'))
          this.$router.push(`/database/${this.$route.params.database_id}/view/${view.id}/data`)
        })
        .catch((error) => {
          this.$toast.error(localizedMessage(this.$t, error, this.$t('error.view.create')))
          this.loadingQuery = false
        })
        .finally(() => {
          this.loadingQuery = false
        })
    },
    buildQuery () {
      if (!this.table) {
        return
      }
      const queryService = useQueryService()
      const { error, reason, column, raw, formatted } = queryService.build(this.table.internal_name, this.select, this.clauses)
      if (error) {
        this.$toast.error(this.$t('error.query.' + reason) + ' ' + column)
        return
      }
      this.query.raw = raw
      if (this.isView) {
        this.view.query = raw
      }
      this.query.formatted = formatted
    },
    canAdd (idx) {
      return idx === this.clauses.length - 1
    },
    addFirst () {
      const column = (this.columnNames && this.columnNames.length) ? this.columnNames[0] : ''
      this.clauses.push({ type: 'where', params: [column, '=', ''] })
    },
    addAnd () {
      this.clauses.push({ type: 'and' })
      this.addFirst()
    },
    addOr () {
      this.clauses.push({ type: 'or' })
      this.addFirst()
    },
    remove (idx) {
      if (idx === 0) {
        if (this.clauses.length === 1) {
          this.clauses.splice(idx, 1)
        } else {
          this.clauses.splice(idx, 2)
        }
      } else {
        // remove current and previous
        this.clauses.splice(idx - 1, 2)
      }
    },
    updateSql (event) {
      const { raw } = event
      if (raw) {
        this.query.raw = raw
        if (this.isView) {
          this.view.query = raw
        }
        this.query.formatted = format(raw, {
          language: 'mysql',
          keywordCase: 'upper'
        })
      }
    }
  }
}
</script>
<style lang="scss">
.text-center {
  text-align: center;
}
</style>
