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
    canViewSchema () {
      if (this.error) {
        return false
      }
      return this.database
    }
  }
}
</script>
