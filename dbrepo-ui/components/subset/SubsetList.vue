<template>
  <div>
    <v-toolbar
      class="pr-2"
      color="secondary"
      flat>
      <v-spacer />
      <v-btn
        :variant="buttonVariant"
        :prepend-icon="filterIcon"
        @click="switchFilter">
        {{ filterText }}
      </v-btn>
    </v-toolbar>
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
        v-if="!loadingSubsets"
        v-for="(subset, i) in subsets"
        :key="`q-${i}`">
        <v-divider v-if="i !== 0" class="mx-4" />
        <v-list>
          <v-list-item
            lines="two"
            :title="title(subset)"
            :subtitle="subtitle(subset)"
            :class="clazz(subset)"
            :to="link(subset)"
            :href="link(subset)">
            <template v-slot:append>
              <ResourceStatus
                :resource="subset" />
            </template>
          </v-list-item>
        </v-list>
      </div>
    </v-card>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'
import { formatTimestampUTCLabel } from '@/utils'

export default {
  data () {
    return {
      loadingSubsets: false,
      loadingIdentifiers: false,
      subsets: [],
      filter: null,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    isContrastTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast')
    },
    isDarkTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().startsWith('dark')
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
    },
    filterIcon () {
      if (this.filter === true) {
        return 'mdi-star'
      }
      if (this.filter === false) {
        return 'mdi-star-off'
      }
      return 'mdi-star-outline'
    },
    filterText () {
      if (this.filter === true) {
        return 'Starred'
      }
      if (this.filter === false) {
        return 'Not Starred'
      }
      return 'All'
    }
  },
  watch: {
    filter () {
      this.loadQueries()
    }
  },
  mounted () {
    this.loadQueries()
  },
  methods: {
    formatTimestampUTCLabel,
    loadQueries () {
      this.loadingSubsets = true
      const queryService = useQueryService()
      queryService.findAll(this.$route.params.database_id, this.filter)
        .then((subsets) => {
          this.loadingSubsets = false
          this.subsets = subsets.map(subset => {
            subset.is_public = this.database.is_public
            subset.is_schema_public = this.database.is_schema_public
            return subset
          })
        })
        .catch(({code, message}) => {
          this.loadingSubsets = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
    },
    switchFilter () {
      if (this.filter === true) {
        this.filter = false
        return
      }
      if (this.filter === false) {
        this.filter = null
        return
      }
      this.filter = true
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
        return formatTimestampUTCLabel(subset.execution)
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
