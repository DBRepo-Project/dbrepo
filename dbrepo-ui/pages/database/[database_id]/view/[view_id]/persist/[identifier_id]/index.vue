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
    view () {
      return this.cacheStore.getView
    },
    identifier () {
      if (!this.view) {
        return false
      }
      const filter = this.view.identifiers.filter(i => i.id === Number(this.$route.params.identifier_id))
      return filter.length === 1 ? filter[0] : null
    },
    canCreateIdentifier () {
      if (!this.roles) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      if (!this.view) {
        return false
      }
      return this.roles.includes('create-identifier') && this.view.owner.id === this.cacheUser.uid
    },
    canUpdateIdentifier () {
      if (!this.roles) {
        return false
      }
      if (!this.identifier) {
        return false
      }
      return this.roles.includes('modify-identifier-metadata') && this.identifier.owner.id === this.cacheUser.uid
    }
  }
}
</script>
