<template>
  <div v-if="canPersistQuery">
    <Persist type="subset" :database="database" :query="query" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import Persist from '@/components/identifier/Persist.vue'
import UserUtils from '@/api/user.utils'
import QueryService from '@/api/query.service'

export default {
  components: {
    Persist
  },
  data () {
    return {
      loading: false,
      loadingQuery: false,
      query: null,
      isAuthorizationError: false,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        { text: 'Queries', to: `/database/${this.$route.params.database_id}/query`, activeClass: '' },
        {
          text: `${this.$route.params.query_id}`,
          to: `/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`,
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
    canPersistQuery () {
      if (this.loadingQuery || !this.query) {
        return false
      }
      return UserUtils.hasReadAccess(this.access)
    }
  },
  mounted () {
    this.loadQuery()
  },
  methods: {
    loadQuery () {
      this.loadingQuery = true
      return new Promise((resolve, reject) => {
        QueryService.findOne(this.$route.params.database_id, this.$route.params.query_id)
          .then((query) => {
            this.query = query
            resolve(query)
          })
          .catch((error) => {
            if (error.response.status === 405) {
              this.isAuthorizationError = true
            }
            reject(error)
          })
          .finally(() => {
            this.loadingQuery = false
          })
      })
    }
  }
}
</script>
<style>
</style>
