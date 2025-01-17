<template>
  <div
    v-if="canCreateView">
    <Builder mode="view" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Builder from '@/components/subset/Builder.vue'
import { useUserStore } from '@/stores/user.js'

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
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    canCreateView () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database-view')
    }
  }
}
</script>
