<template>
  <div
    v-if="canCreateIdentifier || canUpdateIdentifier">
    <Persist
      type="view"
      :database="database" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '@/components/identifier/Persist.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
          to: `/database/${this.$route.params.database_id}/view`,
        },
        {
          title: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/info`,
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/persist`,
        },
        {
          title: `${this.$route.params.identifier_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/persist/${this.$route.params.identifier_id}`,
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
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    canCreateIdentifier () {
      if (!this.roles) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      return this.roles.includes('create-identifier')
    },
    canUpdateIdentifier () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('modify-identifier-metadata')
    }
  }
}
</script>
