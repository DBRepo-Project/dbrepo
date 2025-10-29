<template>
  <div>
    <v-data-table-server
      flat
      v-model="selection"
      :headers="headers"
      :loading="loading || loadingCount || loadingExecute"
      :options="options"
      :items="result.rows"
      :items-length="total"
      :footer-props="footerProps"
      :show-select="select"
      return-object
      :items-per-page-options="footerProps.itemsPerPageOptions"
      @update:options="updateOptions" />
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
      return []
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
            if (typeof code !== 'string') {
              toast.error(message)
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.loadingExecute = false
          })
      } else if (this.type === 'table') {
        const tableService = useTableService()
        tableService.getData(this.$route.params.database_id, id, (this.options.page - 1), this.options.itemsPerPage, timestamp ? timestamp : this.timestamp)
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
          title: header,
          value: header,
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
    },
    resetSelection () {
      this.selection = []
    }
  }
}
</script>
<style>
.v-data-table {
  border-radius: 0;
}
</style>
