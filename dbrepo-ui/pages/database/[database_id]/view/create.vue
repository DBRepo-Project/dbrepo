<template>
  <div
    v-if="canCreateView">
    <Builder mode="view" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Builder from '@/components/subset/Builder.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    Builder
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
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/view`
        },
        {
          title: this.$t('navigation.create'),
          to: `/database/${this.$route.params.database_id}/view/create`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    access () {
      return this.cacheStore.getAccess
    },
    roles () {
      return this.cacheStore.getRoles
    },
    canCreateView () {
      if (!this.roles || !this.roles.includes('create-database-view')) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  }
}
</script>
