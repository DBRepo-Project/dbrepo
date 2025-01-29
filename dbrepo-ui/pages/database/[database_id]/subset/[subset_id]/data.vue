<template>
  <div
    v-if="canViewSubsetData">
    <SubsetToolbar />
    <v-toolbar
      color="secondary"
      flat>
      <v-toolbar-title>
        <v-skeleton-loader
          v-if="loadingSubset"
          type="subtitle"
          color="secondary"
          width="500" />
        <span
          v-else>
          {{ executionUTC }}
        </span>
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        v-if="canViewSubsetData"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-download' : null"
        variant="flat"
        :loading="downloadLoading"
        :text="$t('toolbars.table.data.download')"
        class="mr-2"
        @click.stop="download" />
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-refresh' : null"
        variant="flat"
        :text="$t('toolbars.table.data.refresh')"
        class="mr-2"
        :disabled="loadingSubset"
        :loading="loadingSubset"
        @click="loadSubset" />
    </v-toolbar>
    <v-card
      v-if="subset"
      tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
        :loading="loadingSubset"
        v-model="subset.id"
        type="query"
        class="mt-0 mb-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import QueryResults from '@/components/subset/Results.vue'
import SubsetToolbar from '@/components/subset/SubsetToolbar.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    SubsetToolbar,
    QueryResults
  },
  data () {
    return {
      loadingSubset: false,
      downloadLoading: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}`
        },
        {
         title: this.$t('navigation.subsets'),
          to: `/database/${this.$route.params.database_id}/subset`
        },
        {
          title: `${this.$route.params.subset_id}`,
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}`
        },
        {
          title: this.$t('navigation.data'),
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/data`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    subset () {
      return this.cacheStore.getSubset
    },
    access () {
      return this.cacheStore.getAccess
    },
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
    },
    canViewSubsetData () {
      if (!this.database || !this.subset) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  },
  mounted () {
    this.loadSubset()
  },
  methods: {
    loadSubset () {
      this.loadingSubset = true
      const queryService = useQueryService()
      queryService.findOne(this.$route.params.database_id, this.$route.params.subset_id)
        .then((subset) => {
          this.subset = subset
          this.$refs.queryResults.reExecute(subset.id)
          this.$refs.queryResults.reExecuteCount(subset.id)
          this.loadingSubset = false
        })
        .catch(() => {
          this.loadingSubset = false
        })
        .finally(() => {
          this.loadingSubset = false
        })
    },
    download () {
      this.downloadLoading = true
      const queryService = useQueryService()
      queryService.exportCsv(this.$route.params.database_id, this.subset.id)
        .then((data) => {
          this.downloadLoading = false
          const url = URL.createObjectURL(data)
          const link = document.createElement('a')
          link.href = url
          link.download = 'subset.csv'
          document.body.appendChild(link)
          link.click()
        })
        .catch(({code}) => {
          this.downloadLoading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.downloadLoading = false
        })
    }
  }
}
</script>
