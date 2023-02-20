<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && views.length === 0" flat>
      <v-card-text>
        (no views)
      </v-card-text>
    </v-card>
    <div v-for="(item,i) in views" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list-item-group>
        <v-list-item two-line :class="clazz(item)" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/${item.id}`">
          <v-list-item-content>
            <v-list-item-title v-text="item.name" />
            <v-list-item-subtitle class="mt-2">
              <pre>{{ item.query }}</pre>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list-item-group>
    </div>
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
    database () {
      return this.$store.state.database
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.$store.state.database.views
    },
    isOwner () {
      if (!this.user) {
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
  },
  methods: {
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
        console.debug('deleted view', res.index)
        this.$toast.success(`Successfully deleted view with id ${view.id}`)
      } catch (err) {
        this.$toast.error('Failed to delete view')
        console.error('Failed to delete view')
      }
    },
    clazz (view) {
      if (view.is_public === false) {
        return null
      }
      return 'primary--text'
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
