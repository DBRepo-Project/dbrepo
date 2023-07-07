<template>
  <div v-if="canPersistQuery">
    <Persist type="subset" :database="database" />
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
    hasIdentifier () {
      if ('identifier' in this.database && this.database.identifier) {
        return 'id' in this.database.identifier
      }
      return false
    },
    canPersistQuery () {
      if (this.loadingQuery || !this.query || this.query.is_persisted) {
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
