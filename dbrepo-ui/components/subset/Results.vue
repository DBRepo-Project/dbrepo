<template>
  <div>
    <v-data-table-server
      flat
      :headers="headers"
      :loading="loading || loadingCount || loadingExecute"
      :options="options"
      :items="result.rows"
      :items-length="total"
      :footer-props="footerProps"
      @update:options="updateOptions" />
  </div>
</template>

<script>
export default {
  props: {
    type: {
      type: String,
      default: () => 'query' /* query or view */
    },
    view: {
      type: Object,
      default: () => {
        return {}
      }
    },
    loading: {
      type: Boolean,
      default: () => {
        return false
      }
    }
  },
  data () {
    return {
      loadingCount: false,
      loadingExecute: false,
      resultId: null,
      id: null,
      result: {
        headers: [],
        rows: []
      },
      options: {
        page: 1,
        itemsPerPage: 10
      },
      footerProps: {
        showFirstLastPage: true,
        itemsPerPageOptions: [10, 25, 50, 100]
      },
      total: 0,
    }
  },
  computed: {
    headers () {
      if (this.result.headers.length !== 0) {
        return this.result.headers
      }
      if (this.type === 'view' && this.view && this.view.columns) {
        return this.view.columns.map((c) => {
          return {
            title: c.alias ? c.alias : c.internal_name,
            value: c.alias ? c.alias : c.internal_name,
            sortable: false
          }
        })
      }
      return []
    }
  },
  watch: {
    options: { /* keep */
      handler () {
        this.reExecute(this.id)
      },
      deep: true
    }
  },
  methods: {
    executeFirstTime (parent, sql, timestamp) {
      this.loading++
      const payload = {
        statement: sql,
        timestamp
      }
      const queryService = useQueryService()
      queryService.execute(this.$route.params.database_id, payload, this.options.page - 1, this.options.itemsPerPage)
        .then((result) => {
          this.mapResults(result)
          parent.resultId = result.id
          this.id = result.id
        })
        .finally(() => {
          this.loading--
        })
    },
    reExecute (id) {
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
          .catch(({code}) => {
            const toast = useToastInstance()
            toast.error(this.$t(code))
            this.loadingExecute = false
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
          .catch(({code}) => {
            const toast = useToastInstance()
            toast.error(this.$t(code))
            this.loadingExecute = false
          })
          .finally(() => {
            this.loadingExecute = false
          })
      }
    },
    reExecuteCount (id) {
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
          .catch(({code}) => {
            const toast = useToastInstance()
            toast.error(this.$t(code))
            this.loadingCount = false
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
          .catch(({code}) => {
            const toast = useToastInstance()
            toast.error(this.$t(code))
            this.loadingCount = false
          })
          .finally(() => {
            this.loadingCount = false
          })
      }
    },
    mapResults (data) {
      this.result.headers = data.headers.map((h) => {
        return {
          title: Object.keys(h)[0],
          value: Object.keys(h)[0],
          sortable: false
        }
      })
      console.debug('query result', data)
      this.result.rows = data.result
    },
    updateOptions ({ page, itemsPerPage, sortBy }) {
      this.options.page = page
      this.options.itemsPerPage = itemsPerPage
      this.reExecute(this.id)
    }
  }
}
</script>
<style>
.v-data-table {
  border-radius: 0;
}
</style>
