<template>
  <div>
    <v-progress-linear v-if="loadingIdentifiers || loadingQueries" color="primary" :indeterminate="!error" />
    <v-card v-if="queries && identifiers && queries.length === 0 && identifiers.length === 0" flat tile>
      <v-card-text>
        (no subsets)
      </v-card-text>
    </v-card>
    <v-card v-if="isNotReachable" flat tile>
      <v-card-text>
        Failed to load queries: database is not reachable
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
                <v-list-item-title v-text="title(item)" />
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
                <v-list-item-title v-text="title(item)" />
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
import { formatTimestampUTCLabel } from '@/utils'
import QueryService from '@/api/query.service'
import IdentifierService from '@/api/identifier.service'

export default {
  data () {
    return {
      loadingQueries: false,
      loadingIdentifiers: false,
      error: false,
      queries: [],
      identifiers: [],
      isNotReachable: false,
      isAuthorizationError: false
    }
  },
  computed: {
    baseUrl () {
      return `${location.protocol}//${location.host}`
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
  },
  methods: {
    loadIdentifiers () {
      this.loadingIdentifiers = true
      IdentifierService.findAll(this.$route.params.database_id, 'subset')
        .then((identifiers) => {
          this.identifiers = identifiers
        })
        .finally(() => {
          this.loadingIdentifiers = false
        })
    },
    loadQueries () {
      this.loadingQueries = true
      QueryService.findAll(this.$route.params.database_id, true)
        .then((queries) => {
          this.queries = queries
        })
        .catch((error) => {
          if (error.response.status === 405) {
            this.isAuthorizationError = true
            return
          }
          const { code, message } = error
          this.$toast.error(`[${code}] Failed to load queries: ${message}`)
          this.error = true
        })
        .finally(() => {
          this.loadingQueries = false
        })
    },
    title (query) {
      if (!query.identifier || !('titles' in query.identifier)) {
        return formatTimestampUTCLabel(query.created)
      }
      const enTitle = query.identifier.titles.filter(t => t.language).filter(t => t.language === 'en')
      if (enTitle.length !== 1) {
        return query.identifier.titles[0].title
      }
      return enTitle[0].title
    },
    link (queryOrIdentifier) {
      if (queryOrIdentifier.identifier === null) {
        return `/database/${this.$route.params.database_id}/query/${queryOrIdentifier.id}`
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
