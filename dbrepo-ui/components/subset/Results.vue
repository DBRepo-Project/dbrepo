<template>
  <div>
    <v-alert v-if="dataError || countError" type="error" variant="tonal" role="alert" class="mb-3">
      {{ $t(dataError ? dataErrorKey : 'dataState.countFailed') }}
      <v-btn variant="text" @click="retry">{{ $t('dataState.retry') }}</v-btn>
    </v-alert>
    <v-data-table-server
      v-if="!dataError"
      flat
      v-model="selection"
      :headers="result.headers"
      :loading="loading || loadingExecute"
      :options="options"
      :items="result.rows"
      :items-length="total"
      :hide-default-footer="loadingCount || countError"
      :footer-props="footerProps"
      :show-select="select"
      return-object
      :items-per-page-options="footerProps.itemsPerPageOptions"
      @update:options="updateOptions">
      <template #no-data>
        {{ $t(loaded ? 'dataState.empty' : 'dataState.loading') }}
      </template>
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
      dataError: false,
      dataErrorKey: 'dataState.failed',
      countError: false,
      loaded: false,
      dataRequest: 0,
      countRequest: 0,
      requestTimestamp: undefined,
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
    async reExecute (id, timestamp) {
      if (id == null) return
      const request = ++this.dataRequest
      const databaseId = this.$route.params.database_id
      this.id = id
      this.requestTimestamp = timestamp ?? this.requestTimestamp
      this.loadingExecute = true
      this.dataError = false
      this.loaded = false
      this.result = { headers: [], rows: [] }
      try {
        let result
        if (this.type === 'table') {
          const sort = this.options.sortBy?.[0]
          result = await useTableService().getData(databaseId, id, this.options.page - 1,
            this.options.itemsPerPage, this.requestTimestamp ?? this.timestamp, sort?.key ?? null, sort?.order ?? null)
        } else {
          const service = this.type === 'view' ? useViewService() : useQueryService()
          result = await service.reExecuteData(databaseId, id, this.options.page - 1, this.options.itemsPerPage)
        }
        if (request !== this.dataRequest || databaseId !== this.$route.params.database_id) return
        this.mapResults(result)
        this.loaded = true
      } catch (error) {
        if (request === this.dataRequest && databaseId === this.$route.params.database_id) {
          this.dataError = true
          this.dataErrorKey = [401, 403, 'UNAUTHORIZED', 'FORBIDDEN'].includes(error?.status)
            ? 'dataState.private' : [404, 'NOT_FOUND'].includes(error?.status) ? 'dataState.missing' : 'dataState.failed'
        }
      } finally {
        if (request === this.dataRequest) this.loadingExecute = false
      }
    },
    async reExecuteCount (id, timestamp) {
      if (id == null) return
      const request = ++this.countRequest
      const databaseId = this.$route.params.database_id
      this.id = id
      this.requestTimestamp = timestamp ?? this.requestTimestamp
      this.loadingCount = true
      this.countError = false
      try {
        const count = this.type === 'table'
          ? await useTableService().getCount(databaseId, id, this.requestTimestamp ?? this.timestamp)
          : await (this.type === 'view' ? useViewService() : useQueryService()).reExecuteCount(databaseId, id)
        if (request !== this.countRequest || databaseId !== this.$route.params.database_id) return
        this.total = count
      } catch (error) {
        if (request === this.countRequest && databaseId === this.$route.params.database_id) this.countError = true
      } finally {
        if (request === this.countRequest) this.loadingCount = false
      }
    },
    retry () {
      this.reExecute(this.id, this.requestTimestamp)
      this.reExecuteCount(this.id, this.requestTimestamp)
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
