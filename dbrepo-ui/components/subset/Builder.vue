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
    <v-card
      rounded="0"
      variant="flat">
      <v-card-text>
        <v-form
          ref="form"
          @submit.prevent="$refs.form.validate()">
          <v-row
            v-if="isView"
            class="mt-1"
            dense>
            <v-col
              lg="8">
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
            v-if="isView"
            dense>
            <v-col
              lg="4">
              <v-select
                v-model="view.is_materialized"
                :items="materialOptions"
                persistent-hint
                :variant="inputVariant"
                required
                clearable
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.materialized.label')"
                :hint="$t('pages.database.resource.materialized.hint')" />
            </v-col>
          </v-row>
          <v-row
            v-if="isView"
            dense>
            <v-col
              lg="4">
              <v-select
                v-model="view.is_public"
                :items="dataOptions"
                persistent-hint
                :variant="inputVariant"
                required
                clearable
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.data.label')"
                :hint="$t('pages.database.resource.data.hint')" />
            </v-col>
            <v-col
              lg="4">
              <v-select
                v-model="view.is_schema_public"
                :items="schemaOptions"
                persistent-hint
                :variant="inputVariant"
                required
                clearable
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.schema.label')"
                :hint="$t('pages.database.resource.schema.hint', { resource: 'subset', schema: 'query' })" />
            </v-col>
          </v-row>
          <v-window
            class="mt-4"
            v-model="tabs">
            <v-window-item
              value="0">
              <v-row
                dense>
                <v-col
                  lg="4">
                  <v-select
                    v-model="datasources"
                    :disabled="isExecuted"
                    :items="items"
                    item-title="qualified_name"
                    return-object
                    persistent-hint
                    multiple
                    clearable
                    :variant="inputVariant"
                    :label="$t('pages.view.subpages.create.table.label')"
                    :hint="$t('pages.view.subpages.create.table.hint')"
                    :rules="[v => !!v || $t('validation.required')]" />
                </v-col>
                <v-col
                  lg="4">
                  <v-select
                    v-model="select"
                    :item-title="datasources.length > 1 || joins.length > 0 ? 'qualified_name' : 'internal_name'"
                    :disabled="!datasources || isExecuted"
                    :items="columns"
                    :rules="[v => !!v || $t('validation.required')]"
                    required
                    persistent-hint
                    clearable
                    :variant="inputVariant"
                    :label="$t('pages.view.subpages.create.columns.label')"
                    :hint="$t('pages.view.subpages.create.columns.hint')"
                    :loading="loadingColumns"
                    return-object
                    multiple>
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
              <div
                v-if="Object.keys(needAliasColumns).length > 0"
                class="mt-5 mb-5">
                <v-row
                  dense>
                  <v-col
                    lg="8"
                    class="text-center">
                      <pre
                        class="text-uppercase">{{ $t('pages.subset.subpages.create.alias.text') }}</pre>
                  </v-col>
                </v-row>
                <div
                  v-for="(columns, idx) in needAliasColumns"
                  :key="idx">
                  <v-row
                    v-for="(column, jdx) in columns"
                    :key="jdx"
                    dense>
                    <v-col
                      lg="4"
                      offset-lg="4">
                      <v-text-field
                        v-model="column.alias"
                        :disabled="clausesDisabled"
                        :hint="$t('validation.required')"
                        :rules="[v => !!v || $t('validation.required')]"
                        required
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.alias.label', { column: column.qualified_name })" />
                    </v-col>
                  </v-row>
                </div>
              </div>
              <div
                v-if="joins.length > 0"
                class="mt-5 mb-5">
                <div
                  v-for="(join, idx) in joins"
                  :key="idx">
                  <v-row
                    dense>
                    <v-col
                      lg="8"
                      class="text-center">
                      <pre
                        class="text-uppercase"
                        v-text="$t(`pages.subset.subpages.create.join.${join.type}`)" />
                    </v-col>
                  </v-row>
                  <div
                    v-for="(conditional, jdx) in join.conditionals"
                    :key="jdx">
                    <v-row
                      v-if="jdx > 0"
                      dense>
                      <v-col
                        lg="8"
                        class="text-center">
                      <pre
                        class="text-uppercase">AND</pre>
                      </v-col>
                    </v-row>
                    <v-row
                      dense>
                      <v-col
                        lg="2">
                        <v-select
                          v-if="jdx === 0"
                          v-model="join.type"
                          :disabled="clausesDisabled"
                          item-title="title"
                          item-value="value"
                          persistent-hint
                          :label="$t('pages.subset.subpages.create.join.type.label')"
                          :items="joinTypes" />
                      </v-col>
                      <v-col
                        lg="2">
                        <v-select
                          v-if="jdx === 0"
                          v-model="join.datasource"
                          :disabled="clausesDisabled"
                          item-title="qualified_name"
                          item-value="id"
                          return-object
                          persistent-hint
                          :label="$t('pages.subset.subpages.create.join.table.label')"
                          :items="joinItems" />
                      </v-col>
                      <v-col
                        lg="2">
                        <v-select
                          v-model="conditional.column_id"
                          :disabled="clausesDisabled"
                          item-title="qualified_name"
                          item-value="id"
                          clearable
                          persistent-hint
                          :label="$t('pages.subset.subpages.create.join.on.label')"
                          :items="columns" />
                      </v-col>
                      <v-col
                        lg="2">
                        <v-select
                          v-model="conditional.foreign_column_id"
                          :disabled="clausesDisabled"
                          item-title="qualified_name"
                          item-value="id"
                          clearable
                          persistent-hint
                          :label="$t('pages.subset.subpages.create.join.value.label')"
                          :items="join?.datasource?.columns" />
                      </v-col>
                      <v-col
                        lg="1">
                        <v-btn
                          :disabled="clausesDisabled"
                          class="mt-4"
                          size="small"
                          color="error"
                          variant="flat"
                          :text="$t('pages.subset.subpages.create.join.remove.text')"
                          @click="removeConditional(idx, join.conditionals, jdx)" />
                      </v-col>
                    </v-row>
                  </div>
                  <v-row
                    dense>
                    <v-col>
                      <v-btn
                        :disabled="clausesDisabled"
                        class="mt-4"
                        size="small"
                        color="tertiary"
                        variant="flat"
                        :text="$t('pages.subset.subpages.create.conditional.text')"
                        @click="addConditional(join)" />
                    </v-col>
                  </v-row>
                </div>
              </div>
              <v-row
                v-if="datasources.length > 0">
                <v-col
                  lg="8">
                  <v-btn
                    size="small"
                    color="tertiary"
                    variant="flat"
                    :text="$t('pages.subset.subpages.create.join.text')"
                    :disabled="clausesDisabled"
                    @click="addFirstJoin" />
                </v-col>
              </v-row>
              <v-row
                v-if="select.length > 0">
                <v-col
                  lg="8">
                  <v-btn
                    v-if="clauses.length === 0"
                    size="small"
                    color="tertiary"
                    variant="flat"
                    :text="$t('pages.subset.subpages.create.filter.text')"
                    :disabled="clausesDisabled"
                    @click="addFirstFilter" />
                  <v-btn
                    v-if="clauses.length === 0 && sorts.length === 0"
                    class="ml-2"
                    size="small"
                    color="tertiary"
                    variant="flat"
                    :text="$t('pages.subset.subpages.create.order.text')"
                    :disabled="clausesDisabled"
                    @click="addFirstSort" />
                </v-col>
              </v-row>
              <div
                class="mb-5">
                <v-row
                  v-if="clauses.length > 0">
                  <v-col
                    lg="8"
                    class="text-center">
                    <pre>FILTER</pre>
                  </v-col>
                </v-row>
                <div v-for="(clause, idx) in clauses" :key="idx">
                  <v-row
                    v-if="clause.type === 'where'">
                    <v-col
                      lg="3">
                      <v-select
                        v-model="clause.params[0]"
                        :disabled="clausesDisabled"
                        item-title="qualified_name"
                        item-value="id"
                        variant="underlined"
                        :rules="[v => !!v || $t('validation.required')]"
                        required
                        clearable
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.column.label')"
                        :hint="$t('pages.subset.subpages.create.filter.column.hint')"
                        :items="columns" />
                    </v-col>
                    <v-col
                      lg="2">
                      <v-select
                        v-model="clause.params[1]"
                        :disabled="clausesDisabled"
                        item-title="value"
                        item-value="value"
                        :rules="[v => !!v || $t('validation.required')]"
                        required
                        clearable
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.operator.label')"
                        :hint="operatorHint(clause.params[1])"
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
                    <v-col
                      lg="3">
                      <v-text-field
                        v-model="clause.params[2]"
                        :disabled="clausesDisabled"
                        :rules="[v => !!v || $t('validation.required')]"
                        required
                        clearable
                        persistent-hint
                        :label="$t('pages.subset.subpages.create.filter.value.label')"
                        :hint="$t('pages.subset.subpages.create.filter.value.hint')" />
                    </v-col>
                    <v-col
                      lg="1">
                      <v-btn
                        :disabled="clausesDisabled"
                        class="mt-4"
                        size="small"
                        color="error"
                        variant="flat"
                        :text="$t('pages.subset.subpages.create.filter.remove.text')"
                        @click="removeFilter(idx)" />
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
                          color="tertiary"
                          size="small"
                          :text="$t('pages.subset.subpages.create.filter.and.text')"
                          @click="addAnd" />
                        <v-btn
                          :disabled="!canAdd(idx) || clausesDisabled"
                          class="mt-2"
                          variant="flat"
                          color="tertiary"
                          size="small"
                          :text="$t('pages.subset.subpages.create.filter.or.text')"
                          @click="addOr" />
                      </v-col>
                    </v-row>
                  </div>
                </div>
              </div>
              <div class="mb-5">
                <v-row
                  v-if="sorts.length > 0">
                  <v-col
                    lg="8"
                    class="text-center">
                    <pre>SORT BY</pre>
                  </v-col>
                </v-row>
                <v-row
                  v-for="(sort, idx) in sorts"
                  key="idx">
                  <v-col
                    lg="4">
                    <v-select
                      v-model="sort.column_id"
                      :disabled="clausesDisabled"
                      item-title="qualified_name"
                      item-value="id"
                      :rules="[v => !!v || $t('validation.required')]"
                      required
                      clearable
                      variant="underlined"
                      persistent-hint
                      :label="$t('pages.subset.subpages.create.filter.column.label')"
                      :hint="$t('pages.subset.subpages.create.filter.column.hint')"
                      :items="columns" />
                  </v-col>
                  <v-col
                    lg="4">
                    <v-select
                      v-model="sort.direction"
                      :disabled="clausesDisabled"
                      item-title="title"
                      item-value="value"
                      clearable
                      persistent-hint
                      :label="$t('pages.subset.subpages.create.order.direction.label')"
                      :items="sortings" />
                  </v-col>
                  <v-col
                    lg="1">
                    <v-btn
                      :disabled="clausesDisabled"
                      class="mt-4"
                      size="small"
                      color="error"
                      variant="flat"
                      :text="$t('pages.subset.subpages.create.order.remove.text')"
                      @click="removeSort(idx)" />
                  </v-col>
                </v-row>
              </div>
              <v-row
                v-if="(sorts.length > 0 || clauses.length > 0) && select.length > 0">
                <v-col
                  lg="8">
                  <v-btn
                    size="small"
                    color="tertiary"
                    variant="flat"
                    :text="$t('pages.subset.subpages.create.order.text')"
                    :disabled="clausesDisabled"
                    @click="addFirstSort" />
                </v-col>
              </v-row>
              <v-row
                v-if="!isView"
                dense>
                <v-col
                  lg="8">
                  <v-switch
                    v-model="isHistoric"
                    :label="timestampLabel" />
                </v-col>
              </v-row>
              <v-row
                v-if="!isView && isHistoric"
                dense>
                <v-col
                  lg="8">
                  <div>
                    <label
                      for="timestamp"
                      v-text="$t('pages.subset.subpages.create.timestamp.label')" />
                  </div>
                  <input
                    v-model="timestamp"
                    id="timestamp"
                    required
                    aria-describedby="timestamp"
                    type="datetime-local" />
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
import Raw from '@/components/subset/Raw.vue'
import Results from '@/components/subset/Results.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    Raw,
    Results,
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
      datasources: [],
      views: [],
      sorts: [],
      joins: [],
      timestamp: null,
      isHistoric: false,
      executeDifferentTimestamp: false,
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      materialOptions: [
        { title: this.$t('pages.database.resource.materialized.enabled'), value: true },
        { title: this.$t('pages.database.resource.materialized.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
      tableDetails: null,
      resultId: null,
      errorKeyword: null,
      query: {
        datasource_ids: [],
        columns: [],
        joins: [],
        orders: [],
        filters: []
      },
      view: {
        is_public: true,
        is_schema_public: true,
        is_materialized: false,
        name: null
      },
      select: [],
      clauses: [],
      tabs: 0,
      sortings: [
        { title: 'Ascending ↓', value: 'asc' },
        { title: 'Descening ↑', value: 'desc' },
      ],
      joinTypes: [
        { title: this.$t('pages.subset.subpages.create.join.inner'), value: 'inner' },
        { title: this.$t('pages.subset.subpages.create.join.left'), value: 'left' },
        { title: this.$t('pages.subset.subpages.create.join.right'), value: 'right' },
        { title: this.$t('pages.subset.subpages.create.join.cross'), value: 'cross' },
      ],
      loadingQuery: false,
      loadingColumns: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    operators () {
      if (!this.database) {
        return []
      }
      return this.database.container.image.operators
    },
    timestampLabel () {
      if (this.isHistoric) {
        return this.$t('pages.subset.subpages.create.timestamp.historic.label')
      }
      return this.$t('pages.subset.subpages.create.timestamp.current.label')
    },
    items () {
      if (!this.database) {
        return []
      }
      return this.database.tables.map(t => {
        t.qualified_name = t.internal_name
        return t
      }).concat(this.database.views.map(v => {
        v.qualified_name = `${v.internal_name} (view)`
        return v
      }))
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
    needAliasColumns () {
      if (!this.select || this.select.length === 0) {
        return {}
      }
      const columns = {}
      this.select.forEach((column) => {
        if (!columns[column.internal_name]) {
          columns[column.internal_name] = []
        }
        columns[column.internal_name].push(column)
      })
      Object.keys(columns).forEach(key => {
        if (columns[key].length <= 1) {
          delete columns[key]
        }
      })
      return columns
    },
    viewNames () {
      if (!this.database) {
        return []
      }
      return this.database.views.map(v => v.internal_name)
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
    unsupported () {
      if (!this.$config.public.database.unsupported) {
        return []
      }
      return this.$config.public.database.unsupported.split(',')
    },
    columns () {
      if (!this.datasources) {
        return []
      }
      const columns = []
      this.datasources.forEach(datasource => {
        datasource.columns.forEach(column => {
          column.qualified_name = datasource.internal_name + '.' + column.internal_name
          columns.push(column)
        })
      })
      this.items.forEach(datasource => {
        this.joins.forEach(join => {
          if (join.datasource?.id !== datasource.id) {
            return
          }
          datasource.columns.forEach(column => {
            column.qualified_name = datasource.internal_name + '.' + column.internal_name
            columns.push(column)
          })
        })
      })
      return columns
    },
    joinItems () {
      return this.items.filter(i => !this.datasources.map(d => d.id).includes(i.id))
    },
    subset () {
      if (!this.datasources || !this.select) {
        return null
      }
      return {
        datasource_ids: this.datasources.map(ds => ds.id),
        columns: this.select.map(column => {
          const alias = Object.keys(this.needAliasColumns).map(key => this.needAliasColumns[key]).flat().filter(c => c.id === column.id)
          return {
            id: column.id,
            alias: alias.length > 0 ? alias[0].alias : null
          }
        }),
        filters: this.clauses ? this.clauses.map(clause => {
          if (clause.type === 'or' || clause.type === 'and') {
            return {
              type: clause.type
            }
          }
          const filtered_column = this.columns.filter(column => column.id === clause.params[0])
          const filtered_operator = this.database.container.image.operators.filter(operator => operator.value === clause.params[1])
          if (!filtered_column || filtered_column.length === 0 || !filtered_operator || filtered_operator.length === 0) {
            return null
          }
          return {
            type: clause.type,
            column_id: filtered_column[0].id,
            operator_id: filtered_operator[0].id,
            value: clause.params[2]
          }
        }) : null,
        joins: this.joins.map(join => {
          return {
            type: join.type,
            datasource_id: join.datasource?.id,
            conditionals: join.conditionals.map(conditional => {
              return {
                column_id: conditional.column_id,
                foreign_column_id: conditional.foreign_column_id
              }
            }),
          }
        }),
        orders: this.sorts
      }
    },
    canExecute () {
      if (this.subset === null || !this.subset.columns || this.subset.columns.length === 0 || !this.$refs.form.isValid) {
        return false
      }
      if (!this.isView && this.isHistoric && !this.timestamp) {
        return false
      }
      if (!this.isView) {
        return true
      }
      return this.view.name !== null && this.view.is_public !== null
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
  mounted () {
    this.selectTable()
    this.selectView()
    this.initViewVisibility()
  },
  watch: {
    needAliasColumns () {
      this.$refs.form.validate()
    }
  },
  methods: {
    initViewVisibility () {
      if (!this.database) {
        return
      }
      this.view.is_public = this.database.is_public
      this.view.is_schema_public = this.database.is_schema_public
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
      const tid = this.$route.query.tid
      const selection = this.datasources.filter(t => t.id === tid)
      if (selection.length === 0) {
        console.warn('Failed to find table with id', tid)
        return
      }
      this.datasource = selection[0]
      console.info('Preselect table with id', tid)
    },
    selectView () {
      if (this.$route.query.vid === undefined) {
        return
      }
      const vid = this.$route.query.vid
      const selection = this.views.filter(v => v.id === vid)
      if (selection.length === 0) {
        console.warn('Failed to find view with id', vid)
      }
      this.datasource = selection[0]
      console.info('Preselect view with id', vid)
    },
    addConditional (join) {
      join.conditionals.push( { column_id: null, foreign_column_id: null } )
    },
    execute () {
      if (this.isView) {
        this.createView()
        return
      }
      const timestamp = !this.timestamp ? null : (this.timestamp + ':00Z')
      /* pre-check */
      this.loadingQuery = true
      const queryService = useQueryService()
      queryService.execute(this.$route.params.database_id, this.subset, timestamp, 0, 1)
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
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
    },
    createView () {
      this.loadingQuery = true
      this.view.query = this.subset
      const viewService = useViewService()
      viewService.create(this.$route.params.database_id, this.view)
        .then((simpleView) => {
          this.resultId = simpleView.id
          viewService.findOne(this.$route.params.database_id, simpleView.id)
            .then(async (view) => {
              this.cacheStore.setView(view)
              const toast = useToastInstance()
              toast.success(this.$t('success.view.create'))
              await this.$router.push(`/database/${this.$route.params.database_id}/view/${view.id}/data`)
              this.loadingQuery = false
            })
            .catch(({code, message}) => {
              this.loadingQuery = false
              const toast = useToastInstance()
              if (typeof code !== 'string') {
                toast.error(message)
                return
              }
              toast.error(this.$t(code))
            })
        })
        .catch(({code, message}) => {
          this.loadingQuery = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
    },
    canAdd (idx) {
      return idx === this.clauses.length - 1
    },
    addFirstJoin () {
      this.joins.push({ type: 'inner', datasource_id: null, conditionals: [ { column_id: null, foreign_column_id: null } ] })
      this.$refs.form.validate()
    },
    addFirstFilter () {
      this.clauses.push({ type: 'where', params: [null, '=', ''] })
      this.$refs.form.validate()
    },
    addFirstSort () {
      this.sorts.push({ column_id: null, direction: null})
      this.$refs.form.validate()
    },
    addAnd () {
      this.clauses.push({ type: 'and' })
      this.addFirstFilter()
    },
    addOr () {
      this.clauses.push({ type: 'or' })
      this.addFirstFilter()
    },
    removeFilter (idx) {
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
    removeConditional (idx, conditionals, jdx) {
      if (conditionals.length === 1) {
        // last one, remove the whole join
        this.joins.splice(idx, 1)
      } else {
        conditionals.splice(jdx, 1)
      }
    },
    removeSort (idx) {
      if (idx === 0) {
        this.sorts.splice(idx, 1)
      } else {
        // remove current and previous
        this.sorts.splice(idx - 1, 1)
      }
    },
    toggleColumns () {
      if (this.select.length !== this.columns.length) {
        this.select = this.columns
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
      if (filter.length !== 1 || filter[0].display_name === filter[0].value) {
        return null
      }
      return filter[0].display_name
    }
  }
}
</script>
<style lang="scss">
#timestamp {
  height: 2rem;
  border-bottom: 1px solid;
}
.text-center {
  text-align: center;
}
</style>
