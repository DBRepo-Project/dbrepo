<template>
  <div v-if="canCreateIdentifier || canUpdateIdentifier">
    <Persist type="database" :database="database" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '@/components/identifier/Persist.vue'

export default {
  components: {
    Persist
  },
  data () {
    return {
      loading: false,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    roles () {
      return this.$store.state.roles
    },
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    hasIdentifier () {
      if (this.database && 'identifier' in this.database && this.database.identifier) {
        return 'id' in this.database.identifier
      }
      return false
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      return this.database.owner.username === this.user.username
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
<style>
</style>
