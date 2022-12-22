<template>
  <v-data-table
    flat
    :headers="result.headers"
    :items="result.rows"
    :loading="loading"
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
      loading: false,
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
      total: 0
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
    async executeFirstTime (parent, sql) {
      this.loading = true
      try {
        const res = await this.$axios.post(this.executeUrl, { statement: sql }, this.config)
        console.debug('query result', res.data)
        this.$toast.success('Successfully executed query')
        this.mapResults(res.data)
        this.loading = false
        parent.resultId = res.data.id
      } catch (err) {
        console.error('Failed to execute query', err.response.data)
        this.$toast.error(err.response.data.message)
        this.loading = false
      }
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
      return `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}` + (this.type === 'view' ? '/view' : '/query') + `/${resultId}/data?${urlParams}`
    },
    async reExecute (id) {
      if (id === null) {
        return
      }
      this.loading = true
      try {
        const res = await this.$axios.get(this.reExecuteUrl(id), this.config)
        this.mapResults(res.data)
        this.id = id
        this.loading = false
      } catch (err) {
        console.error('failed to execute query', err)
        this.$toast.error('Failed to execute query: ' + err.response.data.message)
        this.loading = false
      }
    },
    mapResults (data) {
      if (data.result.length) {
        this.result.headers = this.buildHeaders(data.result[0])
      }
      console.debug('query result', data)
      this.result.rows = data.result
      this.total = data.resultNumber
    }
  }
}
</script>

<style scoped>
</style>
