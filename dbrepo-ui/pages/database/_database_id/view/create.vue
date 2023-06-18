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
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        {
          text: 'Views',
          to: `/database/${this.$route.params.database_id}/view`,
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
    canCreateView () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database-view')
    }
  }
}
</script>
