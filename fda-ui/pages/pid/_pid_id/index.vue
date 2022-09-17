<template>
  <div>
    <v-card v-if="!loading" flat>
      <v-card-title>Not Found</v-card-title>
      <v-card-text>
        <p>This PID <code>{{ pid }}</code> cannot be found in the system. Possible reasons are:</p>
        <ul>
          <li>The PID is incorrect in your source.</li>
          <li>The PID was copied incorrectly.</li>
          <li>The PID has not been activated yet.</li>
        </ul>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      loading: false
    }
  },
  computed: {
    pid () {
      return this.$route.params.pid_id
    }
  },
  mounted () {
    this.findPid()
  },
  methods: {
    async findPid () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/pid/${this.$route.params.pid_id}`)
        console.debug('persistent id', res.data)
        this.visitPid(res.data.container_id, res.data.database_id, res.data.query_id, res.data.type)
      } catch (err) {
        console.error('Could not load query', err)
        this.$toast.error('Could not load query')
      }
      this.loading = false
    },
    visitPid (containerId, databaseId, queryId, type) {
      if (!Number.isInteger(containerId) || !Number.isInteger(databaseId)) {
        return false
      }
      this.loading = true
      switch (type) {
        case 'subset':
          if (!Number.isInteger(queryId)) {
            return false
          }
          this.$router.push(`/container/${containerId}/database/${databaseId}/query/${queryId}`)
          break
        case 'database':
          this.$router.push(`/container/${containerId}/database/${databaseId}`)
          break
      }
      return false
    }
  }
}
</script>
