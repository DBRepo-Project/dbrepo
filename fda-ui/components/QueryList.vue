<template>
  <div>
    <v-progress-linear v-if="loading" indeterminate />
    <v-tabs-items>
      <v-card v-if="!loading && queries.length === 0" flat>
        <v-card-title>
          (no subsets)
        </v-card-title>
      </v-card>
      <v-expansion-panels v-if="!loading && queries.length > 0" accordion>
        <v-expansion-panel v-for="(item, i) in queries" :key="i" @click="details(item)">
          <v-expansion-panel-header>
            <pre>{{ item.query }}</pre>
            <v-icon v-if="item.identifier" color="primary" title="Query with metadata" class="pid-icon">mdi-lock-clock</v-icon>
            <v-icon v-if="erroneous(item)" color="error" title="Query failed to execute" class="pid-icon">mdi-flash</v-icon>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-alert
              v-if="erroneous(item)"
              border="left"
              color="error">
              This query failed to execute and did not produce a subset.
            </v-alert>
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
                        {{ executionUTC }}
                      </v-list-item-content>
                    </v-list-item-content>
                  </v-list-item>
                </v-list>
              </v-col>
            </v-row>
            <v-row dense>
              <v-col>
                <v-btn color="secondary" :to="`/container/${$route.params.container_id}/database/${databaseId}/query/${item.id}`">
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
      queries: [],
      identifiers: [],
      queryDetails: {
        id: null,
        doi: null,
        queryHash: null,
        execution: null,
        created: null,
        columns: []
      }
    }
  },
  computed: {
    databaseId () {
      return this.$route.params.database_id
    },
    baseUrl () {
      return 'http://' + location.host
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
    executionUTC () {
      return formatTimestampUTCLabel(this.queryDetails.execution)
    },
    creator () {
      return this.queryDetails.creator
    }
  },
  mounted () {
    this.$root.$on('query-create', this.refresh)
    this.refresh()
  },
  methods: {
    async refresh () {
      // XXX same as in QueryBuilder
      let res
      try {
        this.loading = true
        res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.databaseId}/query`, this.config)
        this.queries = res.data
        console.debug('queries', this.queries)
        try {
          this.loading = true
          const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/identifier`, this.config)
          this.identifiers = res.data
          console.debug('identifiers', this.identifiers)
          this.queries.forEach((query) => {
            const id = this.identifiers.find(id => id.qid === query.id)
            console.debug('id', id)
            if (id !== undefined) {
              query.identifier = id
            }
          })
        } catch (err) {
          console.error('Failed to get identifiers', err)
        }
        this.loading = false
      } catch (err) {
        this.$toast.error('Could not list queries.')
      }
    },
    erroneous (query) {
      return !query.result_hash
    },
    details (query) {
      this.queryDetails = query
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
