<template>
  <div v-if="canPersistView">
    <Persist type="view" :database="database" :view="view" />
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
      loadingView: false,
      view: null,
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
    access () {
      return this.$store.state.access
    },
    hasIdentifier () {
      if ('identifier' in this.view && this.view.identifier) {
        return 'id' in this.view.identifier
      }
      return false
    },
    canPersistView () {
      if (this.loadingView || !this.view || this.hasIdentifier) {
        return false
      }
      return UserUtils.hasReadAccess(this.access)
    }
  },
  mounted () {
    this.loadView()
  },
  methods: {
    loadView () {
      this.loadingView = true
      return new Promise((resolve, reject) => {
        QueryService.findView(this.$route.params.database_id, this.$route.params.view_id)
          .then((view) => {
            this.view = view
            resolve(view)
          })
          .catch((error) => {
            reject(error)
          })
          .finally(() => {
            this.loadingView = false
          })
      })
    }
  }
}
</script>
<style>
</style>
