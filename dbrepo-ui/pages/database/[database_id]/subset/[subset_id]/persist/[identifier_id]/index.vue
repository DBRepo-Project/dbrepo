<template>
  <div
    v-if="canPersistIdentifier || canUpdateIdentifier">
    <Persist
      type="subset"
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
          title: this.$t('navigation.subsets'),
          to: `/database/${this.$route.params.database_id}/subset`,
        },
        {
          title: `${this.$route.params.subset_id}`,
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/info`,
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/persist`,
        },
        {
          title: `${this.$route.params.identifier_id}`,
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/persist/${this.$route.params.identifier_id}`,
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
    subset () {
      return this.cacheStore.getSubset
    },
    access () {
      return this.cacheStore.getAccess
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    identifier () {
      if (!this.subset) {
        return false
      }
      const filter = this.subset.identifiers.filter(i => i.id === Number(this.$route.params.identifier_id))
      return filter.length === 1 ? filter[0] : null
    },
    canPersistIdentifier () {
      if (!this.subset || !this.roles || !this.cacheUser || !this.access) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      if (!this.roles.includes('create-identifier')) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access) && this.subset.owner.id === this.cacheUser.uid
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
