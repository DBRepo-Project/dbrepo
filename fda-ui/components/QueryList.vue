<template>
  <div>
    <v-progress-linear v-if="loading || error" :color="loadingColor" :value="loadProgress" />
    <v-tabs-items>
      <v-card v-if="!loading && queries.length === 0" flat>
        <v-card-text v-text="emptyMessage" />
      </v-card>
      <v-expansion-panels v-if="queries.length > 0" accordion>
        <v-expansion-panel v-for="(item, i) in queries" :key="i" @click="details(item)">
          <v-expansion-panel-header>
            <pre>{{ item.query }}</pre>
            <v-icon v-if="item.type === 'view'" title="Query from a view" class="pid-icon">mdi-gauge</v-icon>
            <v-icon v-if="item.identifier" color="primary" title="Query with metadata" class="pid-icon">mdi-lock-clock</v-icon>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-row dense>
              <v-col>
                <v-list dense>
                  <v-list-item v-if="queryDetails.identifier">
                    <v-list-item-icon>
                      <v-icon>mdi-lock-clock</v-icon>
                    </v-list-item-icon>
                    <v-list-item-content v-if="queryDetails.identifier">
                      <v-list-item-title>
                        Persistent Identifier
                      </v-list-item-title>
                      <v-list-item-content>
                        <a :href="`${baseUrl}/pid/${queryDetails.identifier.id}`">{{ baseUrl }}/pid/{{ queryDetails.identifier.id }}</a>
                      </v-list-item-content>
                      <v-list-item-title class="mt-2">
                        Title
                      </v-list-item-title>
                      <v-list-item-content>
                        {{ queryDetails.identifier.title }}
                      </v-list-item-content>
                    </v-list-item-content>
                  </v-list-item>
                  <v-list-item>
                    <v-list-item-icon>
                      <v-icon>mdi-text-short</v-icon>
                    </v-list-item-icon>
                    <v-list-item-content>
                      <v-list-item-title>
                        Query Statement
                      </v-list-item-title>
                      <v-list-item-content>
                        <pre>{{ queryDetails.query }}</pre>
                      </v-list-item-content>
                      <v-list-item-title class="mt-2">
                        Execution Timestamp
                      </v-list-item-title>
                      <v-list-item-content>
                        {{ createdTime }}
                      </v-list-item-content>
                      <v-list-item-title class="mt-2">
                        Type
                      </v-list-item-title>
                      <v-list-item-content>
                        {{ queryType }}
                      </v-list-item-content>
                    </v-list-item-content>
                  </v-list-item>
                </v-list>
              </v-col>
            </v-row>
            <v-row dense>
              <v-col>
                <v-btn small color="secondary" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/${item.id}`">
                  More
                </v-btn>
              </v-col>
            </v-row>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-tabs-items>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'

export default {
  data () {
    return {
      loading: false,
      loadProgress: 0,
      error: false,
      queries: [],
      queryDetails: {
        id: null,
        doi: null,
        queryHash: null,
        execution: null,
        created: null,
        columns: [],
        type: null
      }
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
    createdTime () {
      return formatTimestampUTCLabel(this.queryDetails.created)
    },
    creator () {
      return this.queryDetails.creator
    },
    queryType () {
      return 'Query' + (this.queryDetails.type === 'view' ? ' was executed by a view' : '')
    },
    emptyMessage () {
      if (this.isPublicOrOwner()) {
        return '(no subsets)'
      }
      return '(private database)'
    }
  },
  mounted () {
    this.loadIdentifiers()
    this.loadQueries()
    this.simulateProgress()
  },
  methods: {
    async loadQueries () {
      if (!this.isPublicOrOwner()) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query?persisted=true`, this.config)
        res.data.forEach((query) => {
          if (this.queries.filter(q => q.id === query.id).length > 0) {
            return
          }
          this.queries.push(query)
        })
        console.debug('queries', this.queries)
      } catch (err) {
        this.error = true
        console.error('Connection to query store failed', err.response.data)
        this.$toast.error(err.response.data.message)
      }
      this.loading = false
    },
    async loadIdentifiers () {
      if (!this.isPublicOrOwner()) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/identifier?dbid=${this.$route.params.database_id}`, this.config)
        const identifiers = res.data.filter(i => i.type === 'subset')
        const queries = identifiers.map((identifier) => {
          const query = {
            id: identifier.query_id,
            identifier,
            type: identifier.type,
            query: identifier.query,
            query_hash: identifier.query_hash,
            result_hash: identifier.result_hash,
            created: identifier.created,
            execution: identifier.execution
          }
          return query
        })
        this.queries = queries
        console.debug('identifier queries', queries)
      } catch (err) {
        console.error('Failed to load identifiers', err.response.data)
        this.$toast.error('Failed to load identifiers')
      }
      this.loading = false
    },
    details (query) {
      this.queryDetails = query
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
