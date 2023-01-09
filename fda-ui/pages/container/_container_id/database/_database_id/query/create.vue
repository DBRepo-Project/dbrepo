<template>
  <div v-if="isResearcher">
    <QueryBuilder />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import { isResearcher } from '@/utils'

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
    isResearcher () {
      return isResearcher(this.user)
    }
  }
}
</script>
