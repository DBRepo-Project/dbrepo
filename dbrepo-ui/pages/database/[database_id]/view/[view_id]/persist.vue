<template>
  <div v-if="canPersistView">
    <Persist type="view" :database="database" :view="view" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '@/components/identifier/Persist'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    Persist
  },
  data () {
    return {
      loading: false,
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
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/view`
        },
        {
          title: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/persist`,
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
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    access () {
      return this.userStore.getAccess
    },
    canPersistView () {
      if (!this.view) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  }
}
</script>
<style>
</style>
