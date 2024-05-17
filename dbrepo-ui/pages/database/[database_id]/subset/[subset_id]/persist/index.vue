<template>
  <div v-if="canPersistQuery">
    <Persist type="subset" :database="database" :query="query" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '~/components/identifier/Persist.vue'
import { useUserStore } from '~/stores/user.js'
import { useCacheStore } from '~/stores/cache.js'

export default {
  components: {
    Persist
  },
  data () {
    return {
      loading: false,
      loadingQuery: false,
      query: null,
      isAuthorizationError: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
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
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/persist`,
          disabled: true
        }
      ],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    roles () {
      return this.userStore.getRoles
    },
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.userStore.getAccess
    },
    canPersistQuery () {
      if (this.loadingQuery || !this.query) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  },
  mounted () {
    this.loadQuery()
  },
  methods: {
    loadQuery () {
      this.loadingQuery = true
      return new Promise((resolve, reject) => {
        const queryService = useQueryService()
        queryService.findOne(this.$route.params.database_id, this.$route.params.subset_id)
          .then((query) => {
            this.query = query
            resolve(query)
          })
          .catch((error) => {
            if (error.response.status === 405) {
              this.isAuthorizationError = true
            }
            reject(error)
          })
          .finally(() => {
            this.loadingQuery = false
          })
      })
    }
  }
}
</script>
<style>
</style>
