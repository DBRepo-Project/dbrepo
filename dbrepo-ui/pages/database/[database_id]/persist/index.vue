<template>
  <div
    v-if="canCreateIdentifier || canUpdateIdentifier">
    <Persist
      type="database"
      :database="database" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const { loggedIn, user, login, logout } = useOidcAuth()
const userInfo = ref(loggedIn ? user.value?.userInfo : null)
const roles = ref(loggedIn ? user.value?.claims?.realm_access?.roles : [])
</script>
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
    hasIdentifier () {
      if (this.database && 'identifier' in this.database && this.database.identifier) {
        return 'id' in this.database.identifier
      }
      return false
    },
    isOwner () {
      if (!this.database || !this.userInfo) {
        return false
      }
      return this.database.owner.id === this.userInfo.uid
    },
    canCreateIdentifier () {
      if (!this.roles || this.hasIdentifier) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      return this.roles.includes('create-identifier') && this.isOwner
    },
    canUpdateIdentifier () {
      if (!this.roles) {
        return false
      }
      return this.hasIdentifier && this.roles.includes('modify-identifier-metadata')
    }
  }
}
</script>
