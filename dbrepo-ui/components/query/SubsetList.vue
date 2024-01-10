<template>
  <div>
    <v-progress-linear v-if="loadingIdentifiers || loadingQueries" color="primary" :indeterminate="!error" />
    <v-card v-if="isNotReachable" flat tile>
      <v-card-text>
        Failed to load queries: database is not reachable
      </v-card-text>
    </v-card>
    <v-card v-if="queries.length === 0" flat tile>
      <v-card-text>(no subsets)</v-card-text>
    </v-card>
    <v-tabs-items>
      <div v-for="(item,i) in queries" :key="i">
        <v-divider v-if="i !== 0" class="mx-4" />
        <v-list-item-group>
          <v-list-item two-line :class="clazz(item)" :to="link(item)" :href="link(item)">
            <v-list-item-content>
              <v-list-item-title v-text="title(item)" />
              <v-list-item-subtitle class="mt-2">
                <pre>{{ item.query }}</pre>
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action v-if="item.identifiers.length > 0">
              <v-tooltip left>
                <template v-slot:activator="{ on, attrs }">
                  <v-icon color="primary" v-bind="attrs" v-on="on">mdi-identifier</v-icon>
                </template>
                Subset has persistent identifier
              </v-tooltip>
            </v-list-item-action>
          </v-list-item>
        </v-list-item-group>
      </div>
    </v-tabs-items>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'
import QueryService from '@/api/query.service'
import IdentifierService from '@/api/identifier.service'
import IdentifierMapper from '@/api/identifier.mapper'

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
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    creator () {
      return this.queryDetails.creator
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
      if (query.identifiers.length === 0) {
        return formatTimestampUTCLabel(query.created)
      }
      return IdentifierMapper.identifierPreferEnglishTitle(query.identifiers[0])
    },
    link (query) {
      return `/database/${this.$route.params.database_id}/query/${query.id}/info`
    },
    clazz (query) {
      if (query.identifiers.length > 0) {
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
