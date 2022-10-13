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
    queryId: { type: Number, default: () => 0 }
  },
  data () {
    return {
      parent: null,
      loading: false,
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
    headers () {
      if (this.token === null) {
        return null
      }
      return { Authorization: `Bearer ${this.token}` }
    }
  },
  mounted () {
    this.execute()
  },
  methods: {
    async executeFirstTime (parent) {
      this.parent = parent
      this.loading = true
      try {
        const data = {
          statement: this.parent.sql
        }
        const page = 0
        const urlParams = `page=${page}&size=${this.options.itemsPerPage}`
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query${this.parent.queryId ? `/${this.parent.queryId}` : ''}?${urlParams}`, data, {
          headers: this.headers
        })
        console.debug('query result', res)
        this.$toast.success('Successfully executed query')
        this.mapResults(res.data)
        this.loading = false
        this.parent.queryId = res.data.id
      } catch (err) {
        console.error('query execute', err)
        this.$toast.error('Could not execute query')
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
    async execute () {
      if (this.queryId === 0) {
        return
      }
      this.loading = true
      try {
        const page = this.options.page - 1
        const urlParams = `page=${page}&size=${this.options.itemsPerPage}`
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.queryId}/data?${urlParams}`, {
          headers: this.headers
        })
        this.mapResults(res.data)
        this.loading = false
      } catch (err) {
        if (err.response.status !== 401 && err.response.status !== 405) {
          console.error('query execute', err)
          this.$toast.error('Could not execute query')
        }
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
