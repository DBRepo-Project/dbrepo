<template>
  <div>
    <v-toolbar v-if="db" flat>
      <v-toolbar-title>
        <span>{{ db.name }}</span>
        <v-icon v-if="!db.is_public" color="primary" class="mb-1" title="Private" right>mdi-lock-outline</v-icon>
        <v-icon v-if="db.is_public" class="mb-1" title="Public" right>mdi-lock-open-outline</v-icon>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="token && canModify" class="mr-2 mb-1" :to="`/container/${$route.params.container_id}/database/${databaseId}/table/import`">
          <v-icon left>mdi-cloud-upload</v-icon> Import CSV
        </v-btn>
        <v-btn v-if="token && canModify" color="secondary" class="mr-2 mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${databaseId}/query/create`">
          <v-icon left>mdi-wrench</v-icon> Create Subset
        </v-btn>
        <v-btn v-if="token && canModify" color="secondary" class="mr-2 mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${databaseId}/view/create`">
          <v-icon left>mdi-view-carousel-outline</v-icon> Create View
        </v-btn>
        <v-btn v-if="token && canModify" color="primary" class="mb-1" :to="`/container/${$route.params.container_id}/database/${databaseId}/table/create`">
          <v-icon left>mdi-table-large-plus</v-icon> Create Table
        </v-btn>
      </v-toolbar-title>
      <template v-slot:extension>
        <v-tabs v-model="tab" color="primary">
          <v-tab :to="`/container/${$route.params.container_id}/database/${databaseId}/info`">
            Info
          </v-tab>
          <v-tab :to="`/container/${$route.params.container_id}/database/${databaseId}/table`">
            Tables
          </v-tab>
          <v-tab :to="`/container/${$route.params.container_id}/database/${databaseId}/query`">
            Subsets
          </v-tab>
          <v-tab :to="`/container/${$route.params.container_id}/database/${databaseId}/view`">
            Views
          </v-tab>
          <v-tab v-if="isOwner" :to="`/container/${$route.params.container_id}/database/${databaseId}/settings`">
            Settings
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
import { decodeJwt } from 'jose'

export default {
  data () {
    return {
      tab: null,
      loading: false,
      error: false,
      access: {
        type: null,
        user: {
          username: null
        }
      },
      user: {
        username: null
      },
      database: {
        id: null,
        is_public: null,
        creator: {
          id: null,
          username: null
        }
      }
    }
  },
  computed: {
    databaseId () {
      return this.$route.params.database_id
    },
    loadingColor () {
      return 'primary'
    },
    db () {
      return this.$store.state.db
    },
    token () {
      return this.$store.state.token
    },
    canModify () {
      if (!this.user.username) {
        /* not yet loaded */
        return false
      }
      if (this.database.creator.username === this.user.username) {
        return true
      }
      return this.access.type === 'write_own' || this.access.type === 'write_all'
    },
    isOwner () {
      if (!this.user.username) {
        /* not yet loaded */
        return false
      }
      return this.database.creator.username === this.user.username
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    silentConfig () {
      return {
        headers: this.config.headers,
        progress: false
      }
    },
    isPublicOrOwner () {
      return this.database.is_public || this.database.creator.username === this.user.username
    }
  },
  watch: {
    $route () {
      if (this.database.id !== this.$route.params.database_id) {
        this.loadDatabase()
      }
    }
  },
  mounted () {
    if (this.database.id) {
      return
    }
    this.loadDatabase()
    this.loadUser()
  },
  methods: {
    async loadDatabase () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', res.data)
        this.$store.commit('SET_DATABASE', res.data)
      } catch (err) {
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${this.user.username}`, this.silentConfig)
        this.access = res.data
        console.debug('check access', this.access)
      } catch (err) {
        if (!err.response.status === 401) {
          console.error('Failed to check access', err)
          this.$toast.error('Failed to check access')
        }
      }
      this.loading = false
    }
  }
}
</script>

<style scoped>
#engine-logo {
  width: 2em;
  height: 2em;
  margin-right: 1.25em;
}
</style>
