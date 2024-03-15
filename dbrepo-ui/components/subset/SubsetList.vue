<template>
  <div>
    <v-card
      v-if="isNotReachable"
      variant="flat"
      rounded="0"
      :text="$t('pages.database.subpages.subsets.http')" />
    <v-card
      v-if="queries.length === 0"
      variant="flat"
      rounded="0"
      :text="$t('pages.database.subpages.subsets.empty')" />
    <v-card
      variant="flat"
      rounded="0"
      v-for="(item, i) in queries"
      :key="`q-${i}`">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list>
        <v-list-item
          lines="two"
          :title="title(item)"
          :class="clazz(item)"
          :to="link(item)"
          :href="link(item)">
          <v-list-item-subtitle
            class="mt-2">
            <pre>{{ item.query }}</pre>
          </v-list-item-subtitle>
          <template v-slot:append>
            <v-tooltip
              v-if="item.identifiers.length > 0"
              :text="$t('pages.identifier.pid.title')"
              left>
              <template v-slot:activator="{ props }">
                <v-icon
                  color="primary"
                  v-bind="props">mdi-identifier</v-icon>
              </template>
            </v-tooltip>
          </template>
        </v-list-item>
      </v-list>
    </v-card>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  data () {
    return {
      loadingQueries: false,
      loadingIdentifiers: false,
      error: false,
      queries: [],
      identifiers: [],
      isNotReachable: false,
      isAuthorizationError: false,
      cacheStore: useCacheStore(),
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    }
  },
  mounted () {
    this.loadQueries()
  },
  methods: {
    loadQueries () {
      this.loadingQueries = true
      const queryService = useQueryService()
      queryService.findAll(this.$route.params.database_id, true)
        .then((queries) => {
          this.queries = queries
        })
        .catch((error) => {
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
      const identifierService = useIdentifierService()
      return identifierService.identifierPreferEnglishTitle(query.identifiers[0])
    },
    link (query) {
      return `/database/${this.$route.params.database_id}/subset/${query.id}/info`
    },
    clazz (subset) {
      return this.hasIdentifiers(subset) ? 'primary--text' : null
    },
    hasIdentifiers (subset) {
      return subset && 'identifiers' in subset && subset.identifiers.length > 0
    },
  }
}
</script>

<style lang="scss" scoped>
.pid-icon {
  flex: 0 !important;
  margin-right: 16px;
}
.v-list {
  padding-top: 0;
  padding-bottom: 0;
}
pre {
  white-space: break-spaces;
  overflow: hidden;
}
</style>
