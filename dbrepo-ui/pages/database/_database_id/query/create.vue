<template>
  <div v-if="canExecuteQuery">
    <QueryBuilder />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import UserUtils from '@/api/user.utils'

export default {
  data () {
    return {
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        {
          text: 'Queries',
          to: `/database/${this.$route.params.database_id}/query`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    access () {
      return this.$store.state.access
    },
    canExecuteQuery () {
      if (!this.roles || !this.access) {
        return false
      }
      return UserUtils.hasReadAccess(this.access) && this.roles.includes('execute-query')
    }
  }
}
</script>
