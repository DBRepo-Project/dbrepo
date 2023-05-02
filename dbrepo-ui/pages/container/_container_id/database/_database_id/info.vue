<template>
  <div v-if="database">
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="showIdentifierCard" flat tile>
          <v-card-title>Identifier</v-card-title>
          <v-card-text v-if="hasIdentifier">
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Persistent Identifier
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <Banner v-if="!loading" :identifier="database.identifier" />
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Title
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="paragraph" width="50%" />
                    <span v-if="!loading">{{ identifier.title }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Description
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="paragraph" width="50%" />
                    <span v-if="!loading">{{ identifier.description }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Publisher
                  </v-list-item-title>
                  <v-list-item-content>
                    {{ database.identifier.publisher }}
                  </v-list-item-content>
                  <v-list-item-title v-if="identifier.creators.length > 0" class="mt-2">
                    Creators
                  </v-list-item-title>
                  <v-list-item-content>
                    <p v-for="(person_or_org, i) in identifier.creators" :key="`c-${i}`" class="mt-2">
                      <OrcidIcon v-if="person_or_org.orcid" :orcid="person_or_org.orcid" />
                      <span v-text="`${person_or_org.firstname} ${person_or_org.lastname}`" />
                      <sup v-text="person_or_org.affiliation" />
                    </p>
                    <span v-for="(affiliation, i) in identifier.affiliations" :key="`a-${i}`" class="mt-4">
                      <span>
                        <sup>{{ i+1 }}</sup>
                        {{ affiliation }}
                      </span>
                    </span>
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
                  <v-list-item-title v-if="identifier.related.length > 0" class="mt-2">
                    Related Identifiers
                  </v-list-item-title>
                  <v-list-item-content v-if="identifier.related.length > 0">
                    <div v-for="(rel, i) in identifier.related" :key="`r-${i}`">
                      <span v-if="rel.type === 'DOI'">
                        {{ rel.type }}: <a :href="`https://doi.org/${rel.value}`" target="_blank">{{ rel.value }}</a>
                        <span v-if="rel.relation">({{ rel.relation }})</span>
                      </span>
                      <span v-if="rel.type === 'URL'">
                        {{ rel.type }}: <a :href="`${rel.value}`" target="_blank">{{ rel.value }}</a>
                        <span v-if="rel.relation">({{ rel.relation }})</span>
                      </span>
                      <span v-if="rel.type === 'arXiv'">
                        {{ rel.type }}: <a :href="`https://arxiv.org/abs/${rel.value}`" target="_blank">{{ rel.value }}</a>
                        <span v-if="rel.relation">({{ rel.relation }})</span>
                      </span>
                      <span v-if="rel.type === 'EISSN'">
                        {{ rel.type }}: <a :href="`https://portal.issn.org/resource/ISSN/${rel.value}`" target="_blank">{{ rel.value }}</a>
                        <span v-if="rel.relation">({{ rel.relation }})</span>
                      </span>
                      <span v-if="rel.type !== 'DOI' && rel.type !== 'URL' && rel.type !== 'arXiv' && rel.type !== 'EISSN'">
                        {{ rel.type }}: {{ rel.value }}
                        <span v-if="rel.relation">({{ rel.relation }})</span>
                      </span>
                    </div>
                  </v-list-item-content>
                  <v-list-item-title v-if="identifier.license" class="mt-2">
                    License
                  </v-list-item-title>
                  <v-list-item-content v-if="identifier.license">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <a v-if="identifier.license" target="_blank" :href="identifier.license.uri">{{ identifier.license.identifier }}</a>
                    <span v-if="!identifier.license">(none)</span>
                  </v-list-item-content>
                  <Citation :pid="database.identifier.id" />
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
          <v-card-text v-if="canCreateIdentifier || canDeleteIdentifier">
            <v-card-actions>
              <v-btn
                v-if="canCreateIdentifier"
                small
                color="primary"
                @click="persistDialog = true">
                Get Database PID
              </v-btn>
              <v-btn
                v-if="canEditIdentifier"
                small
                color="secondary"
                @click="persistDialog = true">
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
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-dialog
      v-model="persistDialog"
      persistent
      max-width="860">
      <Persist type="database" :database="database" @close="closePersistDialog" />
    </v-dialog>
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
import DBToolbar from '@/components/DBToolbar'
import Persist from '@/components/dialogs/Persist'
import OrcidIcon from '@/components/icons/OrcidIcon'
import Citation from '@/components/identifier/Citation'
import { formatTimestampUTCLabel } from '@/utils'
import Banner from '@/components/identifier/Banner'
import DatabaseMapper from '@/api/database.mapper'
import DeleteIdentifier from '@/components/dialogs/DeleteIdentifier.vue'

export default {
  components: {
    DeleteIdentifier,
    DBToolbar,
    Persist,
    OrcidIcon,
    Citation,
    Banner
  },
  data () {
    return {
      loading: false,
      loadingDelete: false,
      editDialog: false,
      deleteDialog: false,
      persistDialog: false,
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
    language () {
      return this.database.identifier.language
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
      if (!this.roles) {
        return false
      }
      if (this.hasIdentifier) {
        return false
      }
      return this.roles.includes('create-identifier') || this.roles.includes('create-foreign-identifier')
    },
    canEditIdentifier () {
      if (!this.roles) {
        return false
      }
      if (!this.hasIdentifier) {
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
    publication () {
      if (this.database.identifier.publication_year === null) {
        return null
      } else if (this.database.identifier.publication_month !== null && this.database.identifier.publication_day !== null) {
        return this.database.identifier.publication_year + '-' + this.database.identifier.publication_month + '-' + this.database.identifier.publication_day
      } else {
        return this.database.identifier.publication_year
      }
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
      if (!this.hasIdentifier) {
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
    }
  },
  methods: {
    async closePersistDialog (event) {
      if (event.action === 'persisted') {
        await this.$store.dispatch('reloadDatabase')
      }
      this.persistDialog = false
      this.editVisibilityDialog = false
    },
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
