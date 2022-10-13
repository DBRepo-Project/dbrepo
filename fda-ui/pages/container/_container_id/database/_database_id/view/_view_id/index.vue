<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-skeleton-loader v-if="loadingView" type="text" class="skeleton-small" />
        <span v-if="!loadingView">{{ view.name }}</span>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat tile>
      <v-card-title>
        View Information
      </v-card-title>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-icon>
              <v-icon :color="view_visibility ? 'success' : 'error'">mdi-database-outline</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                View Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="loadingView" type="text" class="skeleton-small" />
                <span v-if="!loadingView && view_visibility">Public</span>
                <span v-if="!loadingView && !view_visibility">Private</span>
              </v-list-item-content>
              <v-list-item-title>
                View Query
              </v-list-item-title>
              <v-list-item-content>
                <pre v-text="view.query" />
              </v-list-item-content>
              <v-list-item-title>
                View Query Hash
              </v-list-item-title>
              <v-list-item-content>
                <pre>sha256:{{ view.query_hash }}</pre>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <QueryResults
      id="query-results"
      ref="queryResults"
      v-model="view.id"
      :query-id="view.id"
      class="mt-0 mb-0" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import { decodeJwt } from 'jose'

export default {
  data () {
    return {
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'View', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view`, activeClass: '' },
        { text: `${this.$route.params.query_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`, activeClass: '' }
      ],
      view: {
        id: parseInt(this.$route.params.view_id),
        query: null,
        query_hash: null,
        is_public: null,
        name: null,
        creator: {
          username: null,
          firstname: null,
          lastname: null
        }
      },
      user: {
        username: null
      },
      loadingView: false,
      error: false
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` },
        progress: false
      }
    },
    is_owner () {
      return this.token && this.view.creator.username === this.user.username
    },
    view_visibility () {
      return this.view.is_public
    },
    creator () {
      if (this.query.creator.username === null) {
        return null
      }
      if (this.query.creator.firstname === null || this.query.creator.lastname === null) {
        return this.query.creator.username
      }
      return this.query.creator.firstname + ' ' + this.query.creator.lastname
    }
  },
  mounted () {
    this.loadUser()
    this.loadView()
  },
  methods: {
    async loadView () {
      this.loadingView = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`, this.config)
        console.debug('view', res.data)
        this.view = res.data
      } catch (err) {
        if (err.response.status !== 401 && err.response.status !== 405) {
          console.error('Could not load query', err)
          this.$toast.error('Could not load query')
        }
        this.error = true
      }
      this.loadingView = false
    },
    loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
    }
  }
}
</script>

<style>
pre {
  white-space: break-spaces;
}
.v-card__text {
  font-size: initial;
}
.skeleton-large .v-skeleton-loader__text {
  width: 400px;
}
.skeleton-medium .v-skeleton-loader__text {
  width: 200px;
}
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
.skeleton-xsmall .v-skeleton-loader__text {
  width: 50px;
}
</style>
