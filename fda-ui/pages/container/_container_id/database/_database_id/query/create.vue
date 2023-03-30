<template>
  <div v-if="canExecuteQuery">
    <QueryBuilder />
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
          text: 'Queries',
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    canExecuteQuery () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('execute-query')
    }
  }
}
</script>
