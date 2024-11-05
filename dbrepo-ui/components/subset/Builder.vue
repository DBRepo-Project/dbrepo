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
        :disabled="!canExecute"
        color="secondary"
        variant="flat"
        class="mr-2"
        :loading="loadingQuery"
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
          ref="form"
          v-model="valid"
          @submit.prevent>
          <v-row
            v-if="isView"
            class="mt-1"
            dense>
            <v-col lg="8">
              <v-text-field
                v-model="view.name"
                :disabled="isExecuted"
                type="text"
                clearable
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => !!v || $t('validation.required'),
                  v => !validViewName(v) || $t('validation.view.exists')
                ]"
                :label="$t('pages.view.subpages.create.name.label')"
                :hint="$t('pages.view.subpages.create.name.hint')" />
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
            <v-col lg="8">
              <v-select
                v-model="view.is_public"
                :items="visibilities"
                persistent-hint
                :variant="inputVariant"
                required
                clearable
                :rules="[
                  v => !!v || $t('validation.required')
                ]"
                :label="$t('pages.view.subpages.create.visibility.label')"
                :hint="$t('pages.view.subpages.create.visibility.hint')" />
            </v-col>
          </v-row>
          <v-window
            class="mt-4"
            v-model="tabs">
            <v-window-item
              value="0">
              <v-row dense>
                <v-col lg="4">
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
                <v-col lg="4">
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
                    @update:model-value="buildQuery">
                    <template
                      v-slot:prepend-item>
                      <v-list-item
                        title="Select All"
                        :active="select.length === columns.length"
                        @click="toggleColumns">
                        <template
                          v-slot:prepend>
                          <v-checkbox-btn
                            :model-value="select.length === columns.length" />
                        </template>
                      </v-list-item>
                      <v-divider
                        class="mt-2" />
                    </template>
                  </v-select>
                </v-col>
              </v-row>
              <v-row v-if="select.length > 0">
                <v-col lg="8">
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
                    lg="8"
                    class="text-center">
                    <pre>FILTER</pre>
                  </v-col>
                </v-row>
                <div v-for="(clause, idx) in clauses" :key="idx">
                  <v-row
                    v-if="clause.type === 'where'">
                    <v-col lg="3">
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
                    <v-col lg="2">
                      <v-select
                        v-model="clause.params[1]"
                        :disabled="clausesDisabled"
                        item-title="value"
                        item-value="value"
                        persistent-hint
                        :label="operatorHint(clause.params[1])"
                        :hint="$t('pages.subset.subpages.create.filter.operator.label')"
                        :items="operators">
                        <template
                          v-slot:append>
                          <NuxtLink
                            target="_blank"
                            :href="documentationLink(clause.params[1])">
                            <v-tooltip
                              location="bottom">
                              <template
                                v-slot:activator="{ props }">
                                <v-icon
                                  v-bind="props"
                                  icon="mdi-help-circle-outline" />
                              </template>
                              {{ $t('navigation.help') }}
                            </v-tooltip>
                          </NuxtLink>
                        </template>
                      </v-select>
                    </v-col>
                    <v-col lg="3">
                      <v-text-field
                        v-model="clause.params[2]"
                        :disabled="clausesDisabled"
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.value.label')"
                        :hint="$t('pages.subset.subpages.create.filter.value.hint')" />
                    </v-col>
                    <v-col lg="1">
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
                      lg="8"
                      class="text-center">
                      <pre>{{ clause.type.toUpperCase() }}</pre>
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
                    <span>
                      {{ $t('pages.subset.subpages.create.expert.warn') }}
                    </span>
                    <pre style="white-space:inherit;">{{ unsupported.join(', ') }}</pre>
                  </v-alert>
                </v-col>
              </v-row>
              <v-row dense>
                <v-col>
                  {{ $t('pages.subset.subpages.create.subtitle') }}
                </v-col>
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
  </div>
</template>

<script>
import TimeDrift from '@/components/TimeDrift.vue'
import Raw from '@/components/subset/Raw.vue'
import Results from '@/components/subset/Results.vue'
import { useCacheStore } from '@/stores/cache'
import { useUserStore } from '@/stores/user'
import { format } from 'sql-formatter'

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
      loadingQuery: false,
      cacheStore: useCacheStore(),
      userStore: useUserStore()
    }
  },
  computed: {
    columnNames () {
      return this.columns && this.columns.map(s => s.internal_name)
    },
    operators () {
      if (!this.database) {
        return []
      }
      return this.database.container.image.operators
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
    columnTypes () {
      if (!this.database) {
        return []
      }
      return this.database.container.image.data_types
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
      return this.sql.includes(';')
    },
    canExecute () {
      if (this.isView) {
        return this.view.name !== null && this.view.is_public !== null && this.view.query !== null
      }
      return this.sql !== null && !this.sql.includes(';')
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
      this.loadingQuery = true
      const queryService = useQueryService()
      queryService.execute(this.$route.params.database_id, { statement: this.sql }, this.timestamp, 0, 1)
        .then(async (subset) => {
          const toast = useToastInstance()
          toast.success(this.$t('success.subset.create'))
          await this.$router.push(`/database/${this.$route.params.database_id}/subset/${subset.id}/data`)
          this.loadingQuery = false
        })
        .catch(({code, message}) => {
          this.loadingQuery = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(`${this.$t(code)}: ${message}`)
        })
    },
    createView () {
      this.loadingQuery = true
      this.view.query = this.sql
      const viewService = useViewService()
      viewService.create(this.$route.params.database_id, this.view)
        .then(async (view) => {
          this.resultId = view.id
          this.cacheStore.reloadDatabase()
          const toast = useToastInstance()
          toast.success(this.$t('success.view.create'))
          await this.$router.push(`/database/${this.$route.params.database_id}/view/${view.id}/data`)
          this.loadingQuery = false
        })
        .catch(({code}) => {
          this.loadingQuery = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
    },
    buildQuery () {
      if (!this.table) {
        return
      }
      const queryService = useQueryService()
      const { error, reason, column, raw, formatted } = queryService.build(this.table.internal_name, this.select, this.columnTypes, this.clauses)
      if (error) {
        const toast = useToastInstance()
        toast.error(this.$t('error.query.' + reason) + ' ' + column)
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
    },
    toggleColumns () {
      if (this.select.length !== this.columns.length) {
        this.select = this.columns
        this.buildQuery()
      } else {
        this.select = []
      }
    },
    documentationLink (value) {
      const filter = this.operators.filter(o => o.value === value)
      if (filter.length !== 1) {
        return null
      }
      return filter[0].documentation
    },
    operatorHint (value) {
      const filter = this.operators.filter(o => o.value === value)
      if (filter.length !== 1) {
        return null
      }
      return filter[0].display_name
    }
  }
}
</script>
<style lang="scss">
.text-center {
  text-align: center;
}
</style>
