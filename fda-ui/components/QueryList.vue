<template>
  <div>
    <v-progress-linear v-if="loadingIdentifiers || loadingQueries || error" :color="loadingColor" :value="loadProgress" />
    <v-card v-if="!(loadingIdentifiers || loadingQueries) && queries && queries.length === 0" flat>
      <v-card-text>
        (no subsets)
      </v-card-text>
    </v-card>
    <v-tabs-items>
      <div v-if="!loadingQueries && !error">
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
              <v-list-item-action v-if="item.identifier">
                <v-icon color="primary">mdi-identifier</v-icon>
              </v-list-item-action>
            </v-list-item>
          </v-list-item-group>
        </div>
      </div>
      <div v-if="!loadingIdentifiers && loadingQueries">
        <!-- show identifiers when loading subsets -->
        <div v-for="(item,i) in identifiers" :key="i">
          <v-divider v-if="i !== 0" class="mx-4" />
          <v-list-item-group>
            <v-list-item two-line :class="clazz(item)" :to="link(item)" :href="navigate(item)">
              <v-list-item-content>
                <v-list-item-title v-text="item.title" />
                <v-list-item-subtitle class="mt-2">
                  <pre>{{ item.query }}</pre>
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-icon color="primary">mdi-identifier</v-icon>
              </v-list-item-action>
            </v-list-item>
          </v-list-item-group>
        </div>
      </div>
      <div v-if="!loadingIdentifiers && !isPublicOrOwner">
        <!-- show identifiers when private -->
        <div v-for="(item,i) in identifiers" :key="i">
          <v-divider v-if="i !== 0" class="mx-4" />
          <v-list-item-group>
            <v-list-item two-line :class="clazz(item)" :to="link(item)" :href="navigate(item)">
              <v-list-item-content>
                <v-list-item-title v-text="item.title" />
                <v-list-item-subtitle class="mt-2">
                  <pre>{{ item.query }}</pre>
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-icon color="primary">mdi-identifier</v-icon>
              </v-list-item-action>
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
      loadingQueries: false,
      loadingIdentifiers: false,
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
        this.loadingIdentifiers = true
        const res = await this.$axios.get(`/api/identifier?dbid=${this.$route.params.database_id}&type=subset`, this.config)
        this.identifiers = res.data
        console.debug('identifiers', this.identifiers)
      } catch (error) {
        this.error = true
        console.error('Failed to load identifiers', error)
        const { message } = error.response
        this.$toast.error(`Failed to load identifiers: ${message}`)
      }
      this.loadingIdentifiers = false
    },
    async loadQueries () {
      try {
        this.loadingQueries = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query?persisted=true`, this.config)
        this.queries = res.data
        console.debug('queries', this.queries)
      } catch (error) {
        const { status, data } = error.response
        const { message } = data
        if (status !== 405) {
          this.error = true
          console.error('Connection to query store failed', error)
          this.$toast.error(`Failed to connect to query store: ${message}`)
        }
      }
      this.loadingQueries = false
    },
    title (query) {
      if (query.identifier === null) {
        return formatTimestampUTCLabel(query.created)
      }
      return query.identifier.title
    },
    link (queryOrIdentifier) {
      if (queryOrIdentifier.identifier === null) {
        return `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${queryOrIdentifier.id}`
      }
      if ('query_id' in queryOrIdentifier) {
        return null
      }
      return null
    },
    navigate (queryOrIdentifier) {
      if (queryOrIdentifier.identifier === null) {
        return
      }
      if ('query_id' in queryOrIdentifier) {
        return `/pid/${queryOrIdentifier.id}`
      }
      return `/pid/${queryOrIdentifier.identifier.id}`
    },
    clazz (queryOrIdentifier) {
      if ('query_id' in queryOrIdentifier || queryOrIdentifier.identifier) {
        return 'primary--text'
      }
      return null
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
