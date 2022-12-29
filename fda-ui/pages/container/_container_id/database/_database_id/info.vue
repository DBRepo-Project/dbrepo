<template>
  <div>
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="hasIdentifier || isCreator" flat tile>
          <v-card-title>Identifier</v-card-title>
          <v-card-text v-if="hasIdentifier">
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title v-if="publisher" class="mt-2">
                    Persistent Identifier
                  </v-list-item-title>
                  <v-list-item-content v-if="publisher">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <a v-if="!loading" :href="pid">{{ pid }}</a>
                  </v-list-item-content>
                  <v-list-item-title v-if="publisher" class="mt-2">
                    Database Publisher
                  </v-list-item-title>
                  <v-list-item-content v-if="publisher">
                    {{ publisher }}
                  </v-list-item-content>
                  <v-list-item-title v-if="identifier.creators.length > 0" class="mt-2">
                    Creators
                  </v-list-item-title>
                  <v-list-item-content>
                    <span v-for="(person_or_org, i) in identifier.creators" :key="`c-${i}`" class="mt-1">
                      <OrcidIcon v-if="person_or_org.orcid" :orcid="person_or_org.orcid" />
                      <span>
                        {{ person_or_org.firstname }} {{ person_or_org.lastname }} <sup>{{ person_or_org.affiliation_id }}</sup>
                      </span>
                    </span>
                    <span v-for="(affiliation, i) in identifier.affiliations" :key="`a-${i}`" class="mt-1">
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
          <v-card-text v-if="isCreator && !loading && !database.identifier.id">
            <v-card-actions>
              <v-btn
                small
                color="primary"
                @click="editDbDialog = true">
                Get Database PID
              </v-btn>
            </v-card-actions>
          </v-card-text>
        </v-card>
        <v-divider v-if="isCreator || hasIdentifier" />
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
                  <v-list-item-title v-if="access.type" class="mt-2">
                    Database Access
                  </v-list-item-title>
                  <v-list-item-content v-if="access.type">
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
            <v-card-actions v-if="isCreator">
              <v-dialog
                v-if="!hasIdentifier"
                v-model="editDbDialog"
                persistent
                max-width="860">
                <Persist type="database" @close="closeDialog" />
              </v-dialog>
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
import Persist from '@/components/dialogs/Persist'
import OrcidIcon from '@/components/icons/OrcidIcon'
import Citation from '@/components/identifier/Citation'
import { formatTimestampUTCLabel, formatUser } from '@/utils'
import { decodeJwt } from 'jose'

export default {
  components: {
    DBToolbar,
    Persist,
    OrcidIcon,
    Citation
  },
  data () {
    return {
      loading: false,
      editDbDialog: false,
      access: {
        type: null,
        user: {
          username: null
        }
      },
      identifier: {
        id: null,
        license: {
          identifier: null,
          uri: null
        },
        creators: []
      },
      metadataLoading: false,
      user: {
        username: null
      },
      database: {
        id: null,
        name: null,
        description: null,
        is_public: null,
        created: null,
        contact: null,
        identifier: {
          id: null,
          license: {
            identifier: null,
            uri: null
          }
        },
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
      if (!this.hasIdentifier) {
        return ''
      }
      return this.identifier.description
    },
    publisher () {
      if (!this.hasIdentifier) {
        return ''
      }
      return this.identifier.publisher
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {
          headers: { Accept: 'application/json' }
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}`, Accept: 'application/json' }
      }
    },
    pid () {
      return `${this.baseUrl}/pid/${this.identifier.id}`
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
      return this.identifier.language
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
      if (this.identifier.publication_year === null) {
        return null
      } else if (this.identifier.publication_month !== null && this.identifier.publication_day !== null) {
        return this.identifier.publication_year + '-' + this.identifier.publication_month + '-' + this.identifier.publication_day
      } else {
        return this.identifier.publication_year
      }
    },
    creator () {
      return formatUser(this.database.creator)
    },
    creatorVerified () {
      return this.database.creator.email_verified
    },
    hasIdentifier () {
      if (this.identifier === null) {
        return false
      }
      return this.identifier.id !== null
    },
    accessDescription () {
      if (!this.access.type) {
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
  mounted () {
    this.loadUser()
    this.loadDatabase()
      .then(() => this.loadIdentifier())
  },
  methods: {
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', res.data)
      } catch (err) {
        this.$toast.error('Could not load database')
      }
      this.loading = false
    },
    closeDialog (event) {
      if (event.action === 'persisted') {
        this.loadDatabase()
      }
      this.editDbDialog = false
      this.editVisibilityDialog = false
    },
    async download () {
      this.metadataLoading = true
      try {
        const config = this.config
        config.headers.Accept = 'text/xml'
        const res = await this.$axios.get(`/api/pid/${this.database.identifier.id}`, config)
        console.debug('export identifier', res)
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'identifier.xml')
        document.body.appendChild(link)
        link.click()
      } catch (err) {
        console.error('Could not export identifier', err)
        this.$toast.error('Could not export identifier')
        this.error = true
      }
      this.metadataLoading = false
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access`, this.config)
        this.access = res.data
        console.debug('check access', this.access)
      } catch (err) {
        const { status } = err.response
        if (status !== 401 && status !== 403) {
          console.error('Failed to check access', err)
          this.$toast.error('Failed to check access')
        }
      }
      this.loading = false
    },
    async loadIdentifier () {
      if (!this.database.identifier.id) {
        return
      }
      this.loadingCitation = true
      try {
        const res = await this.$axios.get(`/api/pid/${this.database.identifier.id}`, this.config)
        this.identifier = res.data
        this.identifier.affiliations = []
        this.identifier.creators.forEach((personOrOrg) => {
          const affiliationId = this.identifier.affiliations.indexOf(personOrOrg.affiliation)
          if (affiliationId === -1) {
            this.identifier.affiliations.push(personOrOrg.affiliation)
            personOrOrg.affiliation_id = this.identifier.affiliations.indexOf(personOrOrg.affiliation) + 1
          } else {
            personOrOrg.affiliation_id = affiliationId + 1
          }
        })
        console.debug('identifier', this.identifier)
      } catch (err) {
        console.error('Failed to load identifier', err)
        this.$toast.error('Failed to load identifier')
      }
    }
  }
}
</script>
<style>
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
.skeleton-large .v-skeleton-loader__text {
  width: 400px;
}
</style>
