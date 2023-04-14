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
        <v-list-item two-line :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/${item.id}`">
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
import DatabaseService from '@/api/database.service'

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
    canDelete () {
      return this.viewDetails.created_by === this.user.id
    }
  },
  mounted () {
  },
  methods: {
    deleteView (view) {
      this.loading = true
      DatabaseService.deleteView(this.$route.params.container_id, this.$route.params.database_id, view.id)
        .then(() => {
          this.$toast.success(`Successfully deleted view with id ${view.id}`)
        })
        .finally(() => {
          this.loading = false
        })
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
