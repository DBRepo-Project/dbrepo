<template>
  <div v-if="canExecuteQuery">
    <Builder />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user'
import Builder from '@/components/subset/Builder'
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
    access () {
      return this.userStore.getAccess
    },
    canExecuteQuery () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('execute-query')
    }
  }
}
</script>
