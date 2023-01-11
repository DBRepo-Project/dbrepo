<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && views.length === 0" flat>
      <v-card-text>
        (no views)
      </v-card-text>
    </v-card>
    <v-expansion-panels v-if="!loading && views.length > 0" v-model="panel" accordion>
      <v-expansion-panel v-for="(item,i) in views" :key="i" @click="details(item)">
        <v-expansion-panel-header>
          {{ item.name }}
        </v-expansion-panel-header>
        <v-expansion-panel-content class="mb-2">
          <v-row dense>
            <v-col>
              <v-list dense>
                <v-list-item>
                  <v-list-item-icon>
                    <v-icon>mdi-text-short</v-icon>
                  </v-list-item-icon>
                  <v-list-item-content>
                    <v-list-item-title>
                      View ID
                    </v-list-item-title>
                    <v-list-item-content v-text="viewDetails.id" />
                    <v-list-item-title class="mt-2">
                      View Query
                    </v-list-item-title>
                    <v-list-item-content>
                      <pre v-text="viewDetails.query" />
                    </v-list-item-content>
                    <v-list-item-title class="mt-2">
                      View Visibility
                    </v-list-item-title>
                    <v-list-item-content>
                      {{ viewVisibility }}
                    </v-list-item-content>
                  </v-list-item-content>
                </v-list-item>
              </v-list>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-btn small color="secondary" class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/${viewDetails.id}`">
                More
              </v-btn>
              <v-btn v-if="isOwner" small color="error" @click="deleteView(viewDetails)">
                Delete
              </v-btn>
            </v-col>
          </v-row>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'

export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      error: false,
      panel: null,
      views: [],
      database: {
        exchange: null,
        is_public: null,
        tables: [],
        creator: {
          username: null
        }
      },
      viewDetails: {
        id: null,
        internal_name: null,
        description: null,
        created: null,
        is_public: null,
        created_by: null
      }
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    user () {
      return this.$store.state.user
    },
    isOwner () {
      if (!this.user.username) {
        /* not yet loaded */
        return false
      }
      return this.database.creator.username === this.user.username
    },
    createdUTC () {
      if (this.viewDetails.created === undefined || this.viewDetails.created === null) {
        return null
      }
      return formatTimestampUTCLabel(this.viewDetails.created)
    },
    viewVisibility () {
      return this.viewDetails.is_public ? 'Public' : 'Private'
    },
    canDelete () {
      console.debug(this.viewDetails.created_by, '=?=', this.user.id)
      return this.viewDetails.created_by === this.user.id
    }
  },
  mounted () {
    this.loadViews()
    this.loadDatabase()
  },
  methods: {
    async loadViews () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view`, this.config)
        this.views = res.data
        console.debug('views', this.views)
      } catch (err) {
        console.error('Failed to load views', err)
      }
      this.loading = false
    },
    async loadDatabase () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', this.database)
      } catch (err) {
        console.error('Failed to load database', err)
      }
      this.loading = false
    },
    async details (table) {
      if (table.id === this.viewDetails.id) {
        /* prevent weird glitch of opening and collapsing simultaneously */
        return
      }
      this.attemptedLoadingConsumers = false
      /* use cache */
      this.viewDetails = table
      /* load remaining info */
      if (this.isPublicOrOwner) {
        try {
          this.loadingDetails = true
          const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${table.id}`, this.config)
          this.viewDetails = res.data
          console.debug('table details', this.viewDetails)
          if (table.id) {
            this.openPanelByTableId(table.id)
            await this.consumerDetails(this.viewDetails.topic)
          }
        } catch (err) {
          this.$toast.error('Failed to load table details')
          console.error('Failed to load table details', err)
        }
        this.loadingDetails = false
      }
    },
    async deleteView (view) {
      try {
        const res = await this.$axios.$delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/view/${view.id}`, this.config)
        console.debug('deleted view', res.data)
        this.$toast.success(`Successfully deleted view with id ${view.id}`)
        await this.loadViews()
      } catch (err) {
        this.$toast.error('Failed to delete view')
        console.error('Failed to delete view')
      }
    }
  }
}
</script>

<style>
.colTable thead th {
  text-align: initial;
}
.colTable tbody tr td {
  padding-left: 0;
}
.align-right {
  text-align: right;
}
.full-width {
  width: 100%;
}
.amqp-consumer {
  display: inline;
}
</style>
