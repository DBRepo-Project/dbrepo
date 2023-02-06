<template>
  <div>
    <v-progress-linear v-if="loadingData || error" :color="loadingColor" :value="loadProgress" />
    <v-data-table
      flat
      :headers="result.headers"
      :items="result.rows"
      :loading="loading"
      :options.sync="options"
      :server-items-length="total" />
  </div>
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
      loadingData: true,
      resultId: null,
      loadProgress: 0,
      id: null,
      result: {
        headers: [],
        rows: []
      },
      error: false,
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
    },
    loadingColor () {
      return this.error ? 'error' : 'primary'
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
  mounted () {
    this.simulateProgress()
  },
  methods: {
    async executeFirstTime (parent, sql) {
      this.loadingData = true
      try {
        const res = await this.$axios.post(this.executeUrl, { statement: sql }, this.config)
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
      this.loadingData = false
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
      this.loadingData = true
      try {
        const res = await this.$axios.get(this.reExecuteUrl(id), this.config)
        this.mapResults(res.data)
        this.id = id
      } catch (error) {
        console.error('failed to execute query', error)
        this.error = true
      }
      this.loadingData = false
    },
    mapResults (data) {
      if (data.result.length) {
        this.result.headers = this.buildHeaders(data.result[0])
      }
      console.debug('query result', data)
      this.result.rows = data.result
      this.total = data.result_number
    },
    simulateProgress () {
      if (this.loadProgress !== 0) {
        return
      }
      const timeout = 30 * 1000 /* ms */
      const ticks = 100 /* ms */
      let i = 0
      setInterval(() => {
        if (i++ >= timeout && !this.error) {
          return
        }
        this.loadProgress = ((i * 100) / timeout) * 100
      }, ticks)
    }
  }
}
</script>
<style>
.v-data-table {
  border-radius: 0;
}
</style>
