<template>
  <v-data-table
    flat
    :headers="result.headers"
    :items="result.rows"
    :loading="loading > 0"
    :options.sync="options"
    :server-items-length="total" />
</template>

<script>
export default {
  props: {
    type: {
      type: String,
      default: () => 'query' /* query or view */
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
    executeUrl () {
      const page = 0
      const urlParams = `page=${page}&size=${this.options.itemsPerPage}`
      return `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query?${urlParams}`
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
    async executeFirstTime (parent, sql, timestamp) {
      this.loading++
      try {
        const res = await this.$axios.post(this.executeUrl, { statement: sql, timestamp }, this.config)
        console.debug('query result', res.data)
        this.$toast.success('Successfully executed query')
        this.mapResults(res.data)
        parent.resultId = res.data.id
      } catch (error) {
        console.error('Failed to execute query', error)
        const { status, data } = error.response
        const { message, code } = data
        if (status === 504) {
          console.error('Failed to execute query: container not online', code)
          this.$toast.error('Failed to execute query: container not online')
        } else {
          console.error('Failed to execute query', code)
          this.$toast.error('Failed to execute query: ' + message)
        }
        this.error = true
      }
      this.loading--
    },
    buildHeaders (firstLine) {
      return Object.keys(firstLine).map(k => ({
        text: k,
        value: k,
        sortable: false
      }))
    },
    reExecuteUrl (resultId) {
      const page = this.options.page - 1
      const urlParams = `page=${page}&size=${this.options.itemsPerPage}`
      return `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/${this.type}/${resultId}/data?${urlParams}`
    },
    reExecuteCountUrl (resultId) {
      return `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/${this.type}/${resultId}/data/count`
    },
    async reExecute (id) {
      if (id === null) {
        return
      }
      this.loading++
      try {
        const res = await this.$axios.get(this.reExecuteUrl(id), this.config)
        this.mapResults(res.data)
        this.id = id
      } catch (error) {
        console.error('failed to execute query', error)
        this.error = true
      }
      this.loading--
    },
    async reExecuteCount (id) {
      if (id === null) {
        return
      }
      this.loading++
      try {
        const res = await this.$axios.get(this.reExecuteCountUrl(id), this.config)
        this.total = res.data
      } catch (error) {
        console.error('failed to execute query count', error)
        this.error = true
      }
      this.loading--
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
