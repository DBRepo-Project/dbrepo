<template>
  <div
    v-if="canCreateSubset">
    <Builder />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const { loggedIn, user, login, logout } = useOidcAuth()
const roles = ref(loggedIn ? user.value?.claims?.realm_access?.roles : [])
</script>
<script>
import Builder from '@/components/subset/Builder.vue'
import {useCacheStore} from '@/stores/cache.js'

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
         title: this.$t('navigation.subsets'),
          to: `/database/${this.$route.params.database_id}/subset`
        },
        {
          title: this.$t('navigation.create'),
          to: `/database/${this.$route.params.database_id}/create`,
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
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canCreateSubset () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      return this.hasReadAccess
    }
  }
}
</script>
