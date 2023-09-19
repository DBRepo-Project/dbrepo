<template>
  <v-data-table
    flat
    :headers="headers"
    :items="result.rows"
    :loading="loading > 0"
    :options.sync="options"
    :server-items-length="total" />
</template>

<script>
import QueryService from '@/api/query.service'
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
    }
  },
  data () {
    return {
      loading: 0,
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
      total: -1
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
            text: c.alias ? c.alias : c.internal_name,
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
      QueryService.execute(this.$route.params.database_id, payload, 0, this.options.itemsPerPage)
        .then((result) => {
          this.mapResults(result)
          parent.resultId = result.id
        })
        .finally(() => {
          this.loading--
        })
    },
    buildHeaders (firstLine) {
      return Object.keys(firstLine).map(k => ({
        text: k,
        value: k,
        sortable: false
      }))
    },
    reExecute (id) {
      if (id === null) {
        return
      }
      this.loading++
      if (this.type === 'query') {
        QueryService.reExecuteQuery(this.$route.params.database_id, id, 0, this.options.itemsPerPage)
          .then((result) => {
            this.mapResults(result)
            this.id = id
          })
          .finally(() => {
            this.loading--
          })
      } else {
        QueryService.reExecuteView(this.$route.params.database_id, id, 0, this.options.itemsPerPage)
          .then((result) => {
            this.mapResults(result)
            this.id = id
          })
          .finally(() => {
            this.loading--
          })
      }
    },
    reExecuteCount (id) {
      if (id === null) {
        return
      }
      this.loading++
      if (this.type === 'query') {
        QueryService.reExecuteQueryCount(this.$route.params.database_id, id)
          .then((count) => {
            this.total = count
          })
          .finally(() => {
            this.loading--
          })
      } else {
        QueryService.reExecuteViewCount(this.$route.params.database_id, id)
          .then((count) => {
            this.total = count
          })
          .finally(() => {
            this.loading--
          })
      }
    },
    mapResults (data) {
      if (data.result.length) {
        this.result.headers = this.buildHeaders(data.result[0])
      }
      console.debug('query result', data)
      this.result.rows = data.result
      if (this.total < 0 && data.result_number != null) {
        this.total = data.result_number
      }
    }
  }
}
</script>
<style>
.v-data-table {
  border-radius: 0;
}
</style>
