<template>
  <div
    v-if="canPersistIdentifier || canUpdateIdentifier">
    <Persist
      type="database"
      :database="database" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
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
          title: 'Persist',
          to: `/database/${this.$route.params.database_id}/persist`,
        },
        {
          title: `${this.$route.params.identifier_id}`,
          to: `/database/${this.$route.params.database_id}/persist/${this.$route.params.identifier_id}`,
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
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    access () {
      return this.cacheStore.getAccess
    },
    identifier () {
      if (!this.database) {
        return false
      }
      const filter = this.database.identifiers.filter(i => i.id === this.$route.params.identifier_id)
      return filter.length === 1 ? filter[0] : null
    },
    canPersistIdentifier () {
      if (!this.database || !this.roles || !this.cacheUser || !this.access) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      if (!this.roles.includes('create-identifier')) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access) && this.database.owner.id === this.cacheUser.uid
    },
    canUpdateIdentifier () {
      if (!this.identifier || !this.roles) {
        return false
      }
      if (this.roles.includes('modify-identifier-metadata')) {
        return true
      }
      if (!this.roles.includes('create-identifier')) {
        return false
      }
      return this.identifier.owner.id === this.cacheUser.uid
    }
  }
}
</script>
