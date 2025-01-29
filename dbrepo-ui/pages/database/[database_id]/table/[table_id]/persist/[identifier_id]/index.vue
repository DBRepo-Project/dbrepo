<template>
  <div
    v-if="canPersistIdentifier || canUpdateIdentifier">
    <Persist type="table" :database="database" />
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
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/table`,
        },
        {
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/info`,
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/persist`,
        },
        {
          title: `${this.$route.params.identifier_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/persist/${this.$route.params.identifier_id}`,
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
    table () {
      return this.cacheStore.getTable
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    access () {
      return this.cacheStore.getAccess
    },
    identifier () {
      if (!this.table) {
        return false
      }
      const filter = this.table.identifiers.filter(i => i.id === Number(this.$route.params.identifier_id))
      return filter.length === 1 ? filter[0] : null
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
      return userService.hasReadAccess(this.access) && this.table.owner.id === this.cacheUser.uid
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
