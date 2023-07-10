<template>
  <div v-if="database">
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <Summary v-if="showIdentifierCard" :identifier="identifier" />
        <v-card flat tile>
          <v-card-text v-if="canCreateIdentifier || canDeleteIdentifier">
            <v-card-actions>
              <v-btn
                v-if="canCreateIdentifier"
                small
                :to="`/database/${$route.params.database_id}/persist`"
                color="primary">
                Get Database PID
              </v-btn>
              <v-btn
                v-if="canEditIdentifier"
                small
                :to="`/database/${$route.params.database_id}/persist`"
                color="secondary">
                Edit Database PID
              </v-btn>
              <v-btn
                v-if="canDeleteIdentifier && hasIdentifier"
                small
                :loading="loadingDelete"
                color="error"
                @click="deleteDialog = true">
                Delete Database PID
              </v-btn>
            </v-card-actions>
          </v-card-text>
        </v-card>
        <v-divider v-if="showIdentifierCard" />
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
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ database.is_public ? 'Public' : 'Private' }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Internal Name
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ internal_name }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Creator
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">
                      {{ creator }} <sup v-if="creatorVerified">
                        <v-icon color="primary" title="E-Mail verified" small>mdi-check-decagram</v-icon>
                      </sup>
                    </span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Creation
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="createdUTC" />
                  </v-list-item-content>
                  <v-list-item-title v-if="access && access.type" class="mt-2">
                    Database Access
                  </v-list-item-title>
                  <v-list-item-content v-if="access && access.type">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    {{ accessDescription.text }}
                  </v-list-item-content>
                  <v-list-item-title v-if="contact" class="mt-2">
                    Database Contact
                  </v-list-item-title>
                  <v-list-item-content v-if="contact">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="contact" />
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card flat tile>
          <v-card-title>Container</v-card-title>
          <v-card-text>
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Container Name
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="container_name" />
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Container Internal Name
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="container_internal_name" />
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Image Name
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="image_name" />
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Image Version
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="image_version" />
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-dialog
      v-model="deleteDialog"
      persistent
      max-width="480">
      <DeleteIdentifier :identifier="identifier" @close="closeDeleteDialog" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DBToolbar from '@/components/DBToolbar.vue'
import { formatTimestampUTCLabel } from '@/utils'
import DatabaseMapper from '@/api/database.mapper'
import Summary from '@/components/identifier/Summary.vue'
import DeleteIdentifier from '@/components/dialogs/DeleteIdentifier.vue'

export default {
  components: {
    DeleteIdentifier,
    DBToolbar,
    Summary
  },
  data () {
    return {
      loading: false,
      loadingDelete: false,
      loadingStart: false,
      loadingStop: false,
      editDialog: false,
      deleteDialog: false,
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
    tab () {
      return 0
    },
    description () {
      if (!this.hasIdentifier) {
        return ''
      }
      return this.database.identifier.description
    },
    publisher () {
      if (!this.hasIdentifier) {
        return ''
      }
      return this.database.identifier.publisher
    },
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    identifier () {
      if (!this.database) {
        return null
      }
      return this.database.identifier
    },
    access () {
      return this.$store.state.access
    },
    database () {
      return this.$store.state.database
    },
    pid () {
      return `${this.baseUrl}/pid/${this.database.identifier.id}`
    },
    createdUTC () {
      return formatTimestampUTCLabel(this.database.created)
    },
    internal_name () {
      return this.database.internal_name
    },
    container_name () {
      return this.database.container.name
    },
    container_internal_name () {
      return this.database.container.internal_name
    },
    image_name () {
      return this.database.container.image.name
    },
    image_version () {
      return this.database.container.image.version
    },
    showIdentifierCard () {
      if (this.hasIdentifier) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.canCreateIdentifier || this.hasIdentifier
    },
    canCreateIdentifier () {
      if (!this.roles || this.hasIdentifier) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      return this.roles.includes('create-identifier') && this.isOwner
    },
    canEditIdentifier () {
      if (!this.roles || !this.hasIdentifier) {
        return false
      }
      return this.roles.includes('modify-identifier-metadata')
    },
    canDeleteIdentifier () {
      if (!this.user || this.hasDoi) {
        return false
      }
      return this.roles.includes('delete-identifier')
    },
    contact () {
      return DatabaseMapper.databaseToContact(this.database)
    },
    creator () {
      return DatabaseMapper.databaseToOwner(this.database)
    },
    creatorVerified () {
      return this.database.creator.email_verified
    },
    hasIdentifier () {
      if ('identifier' in this.database && this.database.identifier) {
        return 'id' in this.database.identifier
      }
      return false
    },
    hasDoi () {
      if (!this.hasIdentifier || !('doi' in this.database.identifier)) {
        return false
      }
      return this.database.identifier.doi !== null
    },
    accessDescription () {
      if (!this.access) {
        return
      }
      switch (this.access.type) {
        case 'read':
          return { text: 'You can read all contents' }
        case 'write_own':
          return { text: 'You can write own tables and read all contents' }
        case 'write_all':
          return { text: 'You have full access' }
        default:
          return { text: null, class: null }
      }
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      return this.database.owner.username === this.user.username
    }
  },
  methods: {
    async closeDeleteDialog (event) {
      if (event.action === 'deleted') {
        await this.$store.dispatch('reloadDatabase')
      }
      this.deleteDialog = false
    }
  }
}
</script>
<style>
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
.skeleton-large .v-skeleton-loader__text {
  width: 400px;
}
</style>
