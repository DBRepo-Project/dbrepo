<template>
  <div>
    <v-data-table-server
      flat
      v-model="selection"
      :headers="result.headers"
      :loading="loading || loadingCount || loadingExecute"
      :options="options"
      :items="result.rows"
      :items-length="total"
      :footer-props="footerProps"
      :show-select="select"
      return-object
      :items-per-page-options="footerProps.itemsPerPageOptions"
      @update:options="updateOptions">
      <template
        v-for="header in primaryKeyHeaders"
        :key="header.key"
        #[`header.${header.key}`]="{ column, isSorted, getSortIcon, sortBy }">
        <div class="v-data-table-header__content">
          <span class="d-inline-flex align-center ga-1">
            <v-tooltip location="top">
              <template #activator="{ props }">
                <v-icon
                  v-bind="props"
                  icon="mdi-key-variant"
                  size="x-small" />
              </template>
              <span>{{ $t('pages.table.subpages.data.primary-key.hint') }}</span>
            </v-tooltip>
            <span>{{ column.title }}</span>
          </span>
          <v-icon
            v-if="column.sortable"
            class="v-data-table-header__sort-icon"
            :icon="getSortIcon(column)" />
          <div
            v-if="isSorted(column) && sortBy.length > 1"
            class="v-data-table-header__sort-badge">
            {{ sortBy.findIndex(entry => entry.key === column.key) + 1 }}
          </div>
        </div>
      </template>
    </v-data-table-server>
  </div>
</template>

<script>
export default {
  props: {
    type: {
      type: String,
      default: () => 'query' /* query, view or table */
    },
    loading: {
      type: Boolean,
      default: () => false
    },
    select: {
      type: Boolean,
      default: () => false
    },
    primaryKeyColumnNames: {
      type: Array,
      default: () => []
    },
    timestamp: {
      type: String,
      default: () => new Date().toISOString()
    }
  },
  data () {
    return {
      loadingCount: false,
      loadingExecute: false,
      resultId: null,
      id: null,
      selection: null,
      result: {
        headers: [],
        rows: []
      },
      options: {
        page: 1,
        itemsPerPage: 10,
        sortBy: []
      },
      footerProps: {
        showFirstLastPage: true,
        itemsPerPageOptions: [10, 25, 50, 100]
      },
      total: 0,
    }
  },
  computed: {
    primaryKeyHeaders () {
      return this.result.headers.filter(header => header.isPrimaryKey)
    }
  },
  watch: {
    options: { /* keep */
      handler () {
        this.reExecute(this.id)
      },
      deep: true
    },
    selection: {
      handler () {
        this.$emit('selection', this.selection)
      }
    }
  },
  methods: {
    reExecute (id, timestamp) {
      if (id === null) {
        return
      }
      this.loadingExecute = true
      if (this.type === 'query') {
        const queryService = useQueryService()
        queryService.reExecuteData(this.$route.params.database_id, id, this.options.page - 1, this.options.itemsPerPage)
          .then((result) => {
            this.mapResults(result)
            this.id = id
            this.loadingExecute = false
          })
          .catch(({code, message}) => {
            this.loadingExecute = false
            const toast = useToastInstance()
            /* prefer field `message` and use field `code` only as fallback */
            if (typeof message !== 'string') {
              toast.error(this.$t(code))
              return
            }
            toast.error(message)
          })
          .finally(() => {
            this.loadingExecute = false
          })
      } else if (this.type === 'table') {
        const tableService = useTableService()
        const activeSort = this.options.sortBy?.[0]
        const sortColumn = activeSort?.key ?? null
        const sortDirection = activeSort?.order ?? null

        tableService.getData(
          this.$route.params.database_id,
          id,
          (this.options.page - 1),
          this.options.itemsPerPage,
          timestamp ? timestamp : this.timestamp,
          sortColumn,
          sortDirection
        )
          .then((result) => {
            this.mapResults(result)
            this.id = id
            this.loadingExecute = false
          })
          .catch(({code, message}) => {
            this.loadingExecute = false
            const toast = useToastInstance()
            /* prefer field `message` and use field `code` only as fallback */
            if (typeof message !== 'string') {
              toast.error(this.$t(code))
              return
            }
            toast.error(message)
          })
          .finally(() => {
            this.loadingExecute = false
          })
      } else {
        const viewService = useViewService()
        viewService.reExecuteData(this.$route.params.database_id, id, this.options.page - 1, this.options.itemsPerPage)
          .then((result) => {
            this.mapResults(result)
            this.id = id
            this.loadingExecute = false
          })
          .catch(({code, message}) => {
            this.loadingExecute = false
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              toast.error(message)
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.loadingExecute = false
          })
      }
    },
    reExecuteCount (id, timestamp) {
      if (id === null) {
        return
      }
      this.loadingCount = true
      if (this.type === 'query') {
        const queryService = useQueryService()
        queryService.reExecuteCount(this.$route.params.database_id, id)
          .then((count) => {
            this.total = count
            this.loadingCount = false
          })
          .catch(({code, message}) => {
            this.loadingCount = false
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              toast.error(message)
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.loadingCount = false
          })
      } else if (this.type === 'table') {
        const tableService = useTableService()
        tableService.getCount(this.$route.params.database_id, id, timestamp ? timestamp : this.timestamp)
          .then((count) => {
            this.total = count
            this.loadingCount = false
          })
          .catch(({code, message}) => {
            this.loadingCount = false
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              toast.error(message)
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.loadingCount = false
          })
      } else {
        const viewService = useViewService()
        viewService.reExecuteCount(this.$route.params.database_id, id)
          .then((count) => {
            this.total = count
            this.loadingCount = false
          })
          .catch(({code, message}) => {
            this.loadingCount = false
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              toast.error(message)
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.loadingCount = false
          })
      }
    },
    mapResults (data) {
      this.result.headers = data.headers.map((header) => {
        return {
          key: header,
          title: header,
          value: header,
          sortable: this.type === 'table',
          isPrimaryKey: this.type === 'table' && this.primaryKeyColumnNames.includes(header)
        }
      })
      console.debug('query result', data)
      this.result.rows = data.result
    },
    updateOptions ({ page, itemsPerPage, sortBy }) {
      const nextSortBy = this.type === 'table' && Array.isArray(sortBy) ? sortBy : []
      const currentSort = this.options.sortBy?.[0] ?? null
      const nextSort = nextSortBy[0] ?? null
      const sortChanged = currentSort?.key !== nextSort?.key || currentSort?.order !== nextSort?.order

      this.options = {
        ...this.options,
        page: sortChanged ? 1 : page,
        itemsPerPage,
        sortBy: nextSortBy
      }
    },
    resetSelection () {
      this.selection = []
    }
  }
}
</script>
<style scoped>
.v-data-table {
  border-radius: 0;
}

.v-data-table :deep(tbody tr:nth-child(even) > td) {
  background-color: rgba(var(--v-theme-on-surface), 0.02);
}

:global(.v-theme--dark) .v-data-table :deep(tbody tr:nth-child(even) > td) {
  background-color: rgba(var(--v-theme-on-surface), 0.04);
}
</style>
