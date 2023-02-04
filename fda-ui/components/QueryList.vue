<template>
  <div>
    <v-progress-linear v-if="loading || error" :color="loadingColor" :value="loadProgress" />
    <v-tabs-items>
      <v-card v-if="!loading && queries.length === 0 && !error" flat>
        <v-card-text v-text="emptyMessage" />
      </v-card>
      <div v-if="!loading && !error">
        <div v-for="(item,i) in queries" :key="i">
          <v-divider v-if="i !== 0" class="mx-4" />
          <v-list-item-group>
            <v-list-item two-line :class="clazz(item)" :to="link(item)" :href="navigate(item)">
              <v-list-item-content>
                <v-list-item-title v-text="title(item)" />
                <v-list-item-subtitle class="mt-2">
                  <pre>{{ item.query }}</pre>
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-tooltip bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <v-icon v-if="item.identifier" color="primary" v-bind="attrs" v-on="on">
                      mdi-lock-clock
                    </v-icon>
                  </template>
                  <span>Persisted</span>
                </v-tooltip>
              </v-list-item-action>
            </v-list-item>
          </v-list-item-group>
        </div>
      </div>
      <div v-if="!loading && error">
        <!-- show identifiers when error -->
        <div v-for="(item,i) in identifiers" :key="i">
          <v-divider v-if="i !== 0" class="mx-4" />
          <v-list-item-group>
            <v-list-item two-line :to="link(item)">
              <v-list-item-content>
                <v-list-item-title v-text="item.title" />
                <v-list-item-subtitle class="mt-2">
                  <pre>{{ item.query }}</pre>
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
          </v-list-item-group>
        </div>
      </div>
    </v-tabs-items>
  </div>
</template>

<script>
import { formatTimestampUTCLabel, formatUser } from '@/utils'

export default {
  data () {
    return {
      loading: false,
      loadProgress: 0,
      error: false,
      queries: [],
      identifiers: []
    }
  },
  computed: {
    baseUrl () {
      return location.protocol + '//' + location.host
    },
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
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    loadingColor () {
      return this.error ? 'error' : 'primary'
    },
    creator () {
      return this.queryDetails.creator
    },
    emptyMessage () {
      if (this.isPublicOrOwner()) {
        return '(no subsets)'
      }
      return '(private database)'
    }
  },
  mounted () {
    this.loadQueries()
    this.loadIdentifiers()
    this.simulateProgress()
  },
  methods: {
    formatCreator (creator) {
      return formatUser(creator)
    },
    async loadIdentifiers () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/identifier?dbid=${this.$route.params.database_id}&type=subset`, this.config)
        this.identifiers = res.data
        console.debug('identifiers', this.identifiers)
      } catch (error) {
        this.error = true
        console.error('Failed to load identifiers', error)
        const { message } = error.response
        this.$toast.error(`Failed to load identifiers: ${message}`)
      }
      this.loading = false
    },
    async loadQueries () {
      if (!this.isPublicOrOwner()) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query?persisted=true`, this.config)
        this.queries = res.data
        console.debug('queries', this.queries)
      } catch (err) {
        this.error = true
        console.error('Connection to query store failed', err.response.index)
        this.$toast.error(err.response.index.message)
      }
      this.loading = false
    },
    title (query) {
      if (query.identifier === null) {
        return formatTimestampUTCLabel(query.created)
      }
      return query.identifier.title
    },
    link (query) {
      if (query.identifier === null) {
        return `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${query.id}`
      }
      return null
    },
    navigate (query) {
      if (query.identifier === null) {
        return
      }
      return `/pid/${query.identifier.id}`
    },
    clazz (query) {
      if (query.identifier === null) {
        return null
      }
      return 'primary--text'
    },
    isPublicOrOwner () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (this.token === null) {
        return false
      }
      if (!this.user) {
        return false
      }
      return this.database.creator.username === this.user.username
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
.pid-icon {
  flex: 0 !important;
  margin-right: 16px;
}
pre {
  white-space: break-spaces;
  overflow: hidden;
}
</style>
