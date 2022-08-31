<template>
  <div>
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat>
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
                    Database Publisher
                  </v-list-item-title>
                  <v-list-item-content>
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
                  <v-list-item-title class="mt-2">
                    Database Description
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="paragraph" width="50%" />
                    <span v-if="!loading">{{ description }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Creator
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">
                      {{ creator }}
                      <sup>
                        <v-icon v-if="database.creator.email_verified" small color="primary">mdi-check-decagram</v-icon>
                      </sup>
                    </span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Created
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ createdUTC }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Language
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ language }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Publication Date
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ publication }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    License
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <a v-if="database.license" target="_blank" :href="database.license.uri">{{ database.license.identifier }}</a>
                    <span v-if="!database.license">(none)</span>
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
            <v-btn color="secondary" @click="editDbDialog = true">Update Metadata</v-btn>
            <v-dialog
              v-model="editDbDialog"
              persistent
              max-width="640">
              <EditDB :database="database" @close-dialog="closeDialog" />
            </v-dialog>
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
import { formatTimestampUTCLabel, formatUser } from '@/utils'

export default {
  components: {
    DBToolbar,
    EditDB
  },
  data () {
    return {
      loading: false,
      editDbDialog: false,
      database: {
        id: null,
        name: null,
        description: null,
        is_public: null,
        publisher: null,
        created: null,
        subject: [],
        language: null,
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
    description () {
      return this.database.description === null ? '(no description)' : this.database.description
    },
    publisher () {
      return this.database.publisher === null ? '(none)' : this.database.publisher
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
    language () {
      return this.database.language === null ? '(none)' : this.database.language
    },
    internal_name () {
      return this.database.internal_name
    },
    publication () {
      return this.database.publication === null ? '(none)' : this.database.publication
    },
    creator () {
      return formatUser(this.database.creator)
    }
  },
  mounted () {
    this.loadDatabase()
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
    closeDialog () {
      this.loadDatabase()
      this.editDbDialog = false
    }
  }
}
</script>
<style>
.v-card__text {
  font-size: initial;
}
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
</style>
