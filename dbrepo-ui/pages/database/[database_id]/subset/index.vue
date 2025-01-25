<template>
  <div
    v-if="canViewSchema">
    <DatabaseToolbar />
    <SubsetList />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import SubsetList from '@/components/subset/SubsetList.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    SubsetList
  },
  data () {
    return {
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
          to: `/database/${this.$route.params.database_id}/subset`,
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
    access () {
      return this.cacheStore.getAccess
    },
    canViewSchema () {
      if (this.error) {
        return false
      }
      if (!this.database) {
        return false
      }
      if (this.database.is_schema_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  }
}
</script>
