<template>
  <div
    v-if="canPersistIdentifier">
    <Persist
      type="table"
      :database="database"
      :table="table" />
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '@/components/identifier/Persist.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    Persist
  },
  data () {
    return {
      loading: false,
      query: null,
      isAuthorizationError: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/persist`,
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
    table () {
      return this.cacheStore.getTable
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    canPersistIdentifier () {
      if (!this.table || !this.roles || !this.cacheUser || !this.access) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      if (!this.roles.includes('create-identifier')) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access) && this.table.owner.username === this.cacheUser.preferred_username
    }
  }
}
</script>
