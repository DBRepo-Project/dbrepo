<template>
  <div>
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
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
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ database.is_public ? 'Public' : 'Private' }}</span>
                  </v-list-item-content>
                  <v-list-item-title v-if="publisher" class="mt-2">
                    Database Publisher
                  </v-list-item-title>
                  <v-list-item-content v-if="publisher">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ publisher }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Internal Name
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ internal_name }}</span>
                  </v-list-item-content>
                  <v-list-item-title v-if="description" class="mt-2">
                    Database Description
                  </v-list-item-title>
                  <v-list-item-content v-if="description">
                    <v-skeleton-loader v-if="loading" type="paragraph" width="50%" />
                    <span v-if="!loading">{{ description }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Created
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="createdUTC" />
                  </v-list-item-content>
                  <v-list-item-title v-if="language" class="mt-2">
                    Language
                  </v-list-item-title>
                  <v-list-item-content v-if="language">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="language" />
                  </v-list-item-content>
                  <v-list-item-title v-if="publication" class="mt-2">
                    Publication Date
                  </v-list-item-title>
                  <v-list-item-content v-if="publication">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading" v-text="publication" />
                  </v-list-item-content>
                  <v-list-item-title v-if="database.license" class="mt-2">
                    License
                  </v-list-item-title>
                  <v-list-item-content v-if="database.license">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <a v-if="database.license" target="_blank" :href="database.license.uri">{{ database.license.identifier }}</a>
                    <span v-if="!database.license">(none)</span>
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
            <v-card-actions v-if="isCreator">
              <v-btn v-if="token" color="secondary" @click="editDbDialog = true">Get Database PID</v-btn>
              <v-dialog
                v-model="editDbDialog"
                persistent
                max-width="640">
                <EditDB :database="database" @close-dialog="closeDialog" />
              </v-dialog>
              <v-dialog
                v-model="editVisibilityDialog"
                max-width="640">
                <EditVisibility :database="database" @close-dialog="closeDialog" />
              </v-dialog>
            </v-card-actions>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="isCreator" flat tile>
          <v-card-title>Modify visibility</v-card-title>
          <v-card-subtitle>Dangerous operation</v-card-subtitle>
          <v-card-text>
            <v-card-actions>
              <v-btn v-if="token" color="error" @click="editVisibilityDialog = true">Modify</v-btn>
            </v-card-actions>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DBToolbar from '@/components/DBToolbar'
import EditDB from '@/components/dialogs/EditDB'
import EditVisibility from '@/components/dialogs/EditVisibility'
import { formatTimestampUTCLabel, formatUser } from '@/utils'
import { decodeJwt } from 'jose'

export default {
  components: {
    EditVisibility,
    DBToolbar,
    EditDB
  },
  data () {
    return {
      loading: false,
      editDbDialog: false,
      editVisibilityDialog: false,
      user: {
        username: null
      },
      database: {
        id: null,
        name: null,
        description: null,
        is_public: null,
        publisher: null,
        created: null,
        publication_year: null,
        publication_month: null,
        publication_day: null,
        subject: [],
        language: null,
        contact: null,
        container: {
          id: null,
          name: null,
          internal_name: null
        },
        license: {
          uri: null,
          identifier: null
        },
        creator: {
          titles_before: null,
          firstname: null,
          lastname: null,
          username: null,
          titles_after: null
        }
      },
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tab () {
      return 0
    },
    baseUrl () {
      return location.protocol + '//' + location.host
    },
    description () {
      return this.database.description
    },
    publisher () {
      return this.database.publisher
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    createdUTC () {
      return formatTimestampUTCLabel(this.database.created)
    },
    isCreator () {
      if (this.database.creator.username === null || this.user.username === null) {
        return false
      }
      return this.database.creator.username === this.user.username
    },
    language () {
      return this.database.language
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
    contact () {
      if (this.database.contact === null || this.database.contact === undefined) {
        return null
      }
      return formatUser(this.database.contact)
    },
    publication () {
      if (this.database.publication_year === null) {
        return null
      } else if (this.database.publication_month !== null && this.database.publication_day !== null) {
        return this.database.publication_year + '-' + this.database.publication_month + '-' + this.database.publication_day
      } else {
        return this.database.publication_year
      }
    },
    creator () {
      return formatUser(this.database.creator)
    },
    creatorVerified () {
      return this.database.creator.email_verified
    }
  },
  mounted () {
    this.loadDatabase()
    this.loadUser()
  },
  methods: {
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', res.data)
      } catch (err) {
        this.$toast.error('Could not load database.')
      }
      this.loading = false
    },
    closeDialog (event) {
      if (event.success) {
        this.loadDatabase()
      }
      this.editDbDialog = false
      this.editVisibilityDialog = false
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
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
</style>
