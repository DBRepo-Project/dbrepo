<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        <span v-if="cachedView.name">{{ cachedView.name }}</span>
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
              <v-icon v-if="!database">mdi-database-outline</v-icon>
              <v-icon v-if="database" :color="database.is_public ? 'success' : 'error'">mdi-database-outline</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Database Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-xsmall" />
                <span v-if="database && database.is_public">Public</span>
                <span v-if="database && !database.is_public">Private</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Database Name
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-small" />
                <span v-if="database">{{ database.name }}</span>
              </v-list-item-content>
              <div v-if="database && database.identifier">
                <v-list-item-title class="mt-2">
                  Database License
                </v-list-item-title>
                <v-list-item-content>
                  <a :href="database.identifier.license.uri">{{ database.identifier.license.identifier }}</a>
                </v-list-item-content>
              </div>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-text-short</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Query Statement
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!cachedView.query" type="text" />
                <v-skeleton-loader v-if="!cachedView.query" type="text" class="skeleton-large" />
                <pre v-if="cachedView.query">{{ cachedView.query }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                View Creator
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!creator" type="text" class="skeleton-medium" />
                <span v-if="creator">{{ creator }}</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                View Creation
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!cachedView.created" type="text" class="skeleton-medium" />
                <span v-if="cachedView.created">{{ formatUTC(cachedView.created) }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon v-if="cachedView.is_public === null">mdi-view-carousel-outline</v-icon>
              <v-icon v-if="cachedView.is_public !== null" :color="cachedView.is_public ? 'success' : 'error'">mdi-view-carousel-outline</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                View Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="cachedView.is_public === null" type="text" class="skeleton-xsmall" />
                <span v-if="cachedView.is_public !== null">{{ cachedView.is_public ? 'Public' : 'Private' }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <QueryResults
      id="query-results"
      ref="queryResults"
      type="view"
      class="mt-0 mb-0" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import { formatTimestampUTCLabel } from '@/utils'
import DatabaseService from '@/api/database.service'
import UserMapper from '@/api/user.mapper'

export default {
  data () {
    return {
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Views', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view`, activeClass: '' },
        { text: `${this.$route.params.view_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`, activeClass: '' }
      ],
      view: {
        id: null /* only loaded if user has access to view */
      },
      loadingView: true,
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
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.$store.state.database.views
    },
    cachedView () {
      if (!this.database) {
        return {
          id: null,
          name: null,
          query: null,
          created: null,
          is_public: null
        }
      }
      const filter = this.views.filter(v => v.id === Number(this.$route.params.view_id))
      return filter.length === 1 ? filter[0] : null
    },
    creator () {
      if (!this.view) {
        return null
      }
      return UserMapper.userToFullName(this.view.creator)
    }
  },
  mounted () {
    this.loadView()
    this.loadResult(this.$route.params.view_id)
  },
  methods: {
    loadView () {
      this.loadingView = true
      DatabaseService.findView(this.$route.params.container_id, this.$route.params.database_id, this.$route.params.view_id)
        .then((view) => {
          this.view = view
        })
        .then(() => {
          this.loadingView = false
        })
    },
    loadResult (viewId) {
      if (!viewId) {
        return
      }
      this.$refs.queryResults.reExecute(viewId)
      this.$refs.queryResults.reExecuteCount(viewId)
    },
    formatUTC (timestamp) {
      return formatTimestampUTCLabel(timestamp)
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
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
</style>
