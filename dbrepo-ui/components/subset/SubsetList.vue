<template>
  <div>
    <v-card
      v-if="!loadingSubsets && subsets.length === 0"
      variant="flat"
      rounded="0"
      :text="$t('pages.database.subpages.subsets.empty')" />
    <v-card
      variant="flat"
      rounded="0">
      <v-list-item
        v-if="loadingSubsets"
        lines="two">
        <Loading />
      </v-list-item>
      <div
        v-for="(item, i) in subsets"
        :key="`q-${i}`">
        <v-divider v-if="i !== 0" class="mx-4" />
        <v-list>
          <v-list-item
            lines="two"
            :title="title(item)"
            :subtitle="subtitle(item)"
            :class="clazz(item)"
            :to="link(item)"
            :href="link(item)">
            <template v-slot:append>
              <v-tooltip
                v-if="hasPublishedIdentifier(item)"
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
      </div>
    </v-card>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'
import { useUserStore } from '@/stores/user.js'
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      loadingSubsets: false,
      loadingIdentifiers: false,
      subsets: [],
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
    },
    isContrastTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast')
    },
    isDarkTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().startsWith('dark')
    },
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
    }
  },
  mounted () {
    this.loadQueries()
  },
  methods: {
    loadQueries () {
      this.loadingSubsets = true
      const queryService = useQueryService()
      queryService.findAll(this.$route.params.database_id, true)
        .then((subsets) => {
          this.loadingSubsets = false
          this.subsets = subsets.map(subset => {
            subset.is_public = this.database.is_public
            subset.is_schema_public = this.database.is_schema_public
            return subset
          })
        })
        .catch(({code}) => {
          this.loadingSubsets = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
    },
    title (subset) {
      if (subset.identifiers.length === 0) {
        return subset.query
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierPreferEnglishTitle(subset.identifiers[0])
    },
    subtitle (subset) {
      if (subset.identifiers.length === 0) {
        return null
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierPreferEnglishDescription(subset.identifiers[0])
    },
    link (subset) {
      return `/database/${this.$route.params.database_id}/subset/${subset.id}/info`
    },
    clazz (subset) {
      return this.hasPublishedIdentifier(subset) ? 'primary-text' : null
    },
    hasPublishedIdentifier (subset) {
      if (!subset.identifiers) {
        return null
      }
      return subset.identifiers.filter(i => i.status === 'published').length > 0
    }
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
