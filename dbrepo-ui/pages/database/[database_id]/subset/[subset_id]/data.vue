<template>
  <div>
    <SubsetToolbar />
    <v-toolbar
      color="secondary"
      :title="executionUTC"
      flat />
    <v-card tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
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
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    SubsetToolbar,
    QueryResults
  },
  data () {
    return {
      loadingSubset: false,
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
      subset: {
        id: this.$route.params.subset_id
      },
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
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
          this.loadResult()
        })
        .catch(() => {
          this.loadingSubset = false
        })
        .finally(() => {
          this.loadingSubset = false
        })
    },
    loadResult () {
      this.$refs.queryResults.reExecute(this.subset.id)
      this.$refs.queryResults.reExecuteCount(this.subset.id)
    }
  }
}
</script>
<style>
</style>
