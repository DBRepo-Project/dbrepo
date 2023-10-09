<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && views.length === 0" flat>
      <v-card-text v-text="emptyText" />
    </v-card>
    <div v-for="(item,i) in views" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list-item-group>
        <v-list-item two-line :class="clazz(item)" :to="link(item)" :href="navigate(item)">
          <v-list-item-content>
            <v-list-item-title v-text="item.name" />
            <v-list-item-subtitle class="mt-2">
              <pre>{{ item.query }}</pre>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action v-if="item.identifier">
            <v-tooltip left>
              <template v-slot:activator="{ on, attrs }">
                <v-icon color="primary" v-bind="attrs" v-on="on">mdi-identifier</v-icon>
              </template>
              View has persistent identifier
            </v-tooltip>
          </v-list-item-action>
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
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    emptyText () {
      const add = this.database && this.database.is_public ? '' : ' public'
      return `(no${add} views)`
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
      DatabaseService.deleteView(this.$route.params.database_id, view.id)
        .then(() => {
          this.$toast.success(`Successfully deleted view with id ${view.id}`)
        })
        .finally(() => {
          this.loading = false
        })
    },
    link (viewOrIdentifier) {
      if (viewOrIdentifier.identifier === null) {
        return `/database/${this.$route.params.database_id}/view/${viewOrIdentifier.id}`
      }
      if ('view_id' in viewOrIdentifier) {
        return null
      }
      return null
    },
    navigate (viewOrIdentifier) {
      if (viewOrIdentifier.identifier === null) {
        return
      }
      if ('query_id' in viewOrIdentifier) {
        return `/pid/${viewOrIdentifier.id}`
      }
      return `/pid/${viewOrIdentifier.identifier.id}`
    },
    clazz (viewOrIdentifier) {
      if ('view_id' in viewOrIdentifier || viewOrIdentifier.identifier) {
        return 'primary--text'
      }
      return null
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
