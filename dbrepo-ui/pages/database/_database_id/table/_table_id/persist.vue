<template>
  <div v-if="canPersistTable">
    <Persist type="table" :database="database" :table="table" />
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
      query: null,
      isAuthorizationError: false,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        { text: 'Tables', to: `/database/${this.$route.params.database_id}/table`, activeClass: '' },
        {
          text: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`,
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
    access () {
      return this.$store.state.access
    },
    table () {
      return this.$store.state.table
    },
    canPersistTable () {
      if (!this.table) {
        return false
      }
      return UserUtils.hasReadAccess(this.access)
    }
  }
}
</script>
<style>
</style>
