<template>
  <div v-if="canPersistView">
    <Persist type="view" :database="database" :view="view" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '@/components/identifier/Persist'
import UserUtils from '@/api/user.utils'

export default {
  components: {
    Persist
  },
  data () {
    return {
      loading: false,
      isAuthorizationError: false,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        { text: 'Views', to: `/database/${this.$route.params.database_id}/view`, activeClass: '' },
        {
          text: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    roles () {
      return this.$store.state.roles
    },
    database () {
      return this.$store.state.database
    },
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    access () {
      return this.$store.state.access
    },
    canPersistView () {
      if (!this.view) {
        return false
      }
      return UserUtils.hasReadAccess(this.access)
    }
  }
}
</script>
<style>
</style>
