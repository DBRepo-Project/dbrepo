<template>
  <div v-if="view">
    <ViewToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
          <Summary v-if="hasIdentifier" :identifier="identifier" />
          <v-card-text v-if="hasIdentifier">
            <Select :identifiers="identifiers" :identifier="identifier" />
          </v-card-text>
        </v-card>
        <v-divider v-if="hasIdentifier" />
        <v-card flat tile>
          <v-card-title>View</v-card-title>
          <v-card-text>
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title>
                    Query Statement
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="!view.query" type="text" />
                    <v-skeleton-loader v-if="!view.query" type="text" class="skeleton-large" />
                    <pre v-if="view.query">{{ view.query }}</pre>
                  </v-list-item-content>
                  <v-list-item-title v-if="canViewView" class="mt-2">
                    View Creator
                  </v-list-item-title>
                  <v-list-item-content v-if="canViewView">
                    <span v-if="creator">{{ creator }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    View Creation
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="!view.created" type="text" class="skeleton-medium" />
                    <span v-if="view.created">{{ formatUTC(view.created) }}</span>
                  </v-list-item-content>
                  <v-list-item-title>
                    View Visibility
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="view.is_public === null" type="text" class="skeleton-xsmall" />
                    <span v-if="view.is_public !== null">{{ view.is_public ? 'Public' : 'Private' }}</span>
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card flat tile>
          <v-card-title>Database</v-card-title>
          <v-card-text>
            <v-list dense>
              <v-list-item>
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
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import { formatTimestampUTCLabel } from '@/utils'
import ViewToolbar from '@/components/ViewToolbar.vue'
import UserMapper from '@/api/user.mapper'
import UserUtils from '@/api/user.utils'
import Summary from '@/components/identifier/Summary.vue'
import Select from '@/components/identifier/Select.vue'

export default {
  components: {
    Select,
    Summary,
    ViewToolbar
  },
  data () {
    return {
      tab: 0,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Views', to: `/database/${this.$route.params.database_id}/view`, activeClass: '' },
        { text: `${this.$route.params.view_id}`, to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`, activeClass: '' },
        { text: 'Info', to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/info`, activeClass: '' }
      ],
      error: false
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    database () {
      return this.$store.state.database
    },
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    access () {
      return this.$store.state.access
    },
    identifiers () {
      if (!this.view) {
        return []
      }
      return this.view.identifiers
    },
    identifier () {
      if (this.pid) {
        const filter = this.identifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.identifiers[0]
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.database.views
    },
    pid () {
      return this.$route.query.pid
    },
    canViewView () {
      /* is private */
      return UserUtils.hasReadAccess(this.access)
    },
    hasIdentifier () {
      return this.identifiers.length > 0
    },
    creator () {
      if (!this.view) {
        return null
      }
      return UserMapper.userToFullName(this.view.creator)
    }
  },
  methods: {
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
