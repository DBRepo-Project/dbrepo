<template>
  <div v-if="canCreateView">
    <QueryBuilder mode="view" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
export default {
  data () {
    return {
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        {
          text: 'Views',
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    canCreateView () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('create-database-view')
    }
  }
}
</script>
