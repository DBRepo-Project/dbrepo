<template>
  <div>
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="hasIdentifier" flat tile>
          <v-card-title>Identifier</v-card-title>
          <v-card-text>
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
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ publisher }}</span>
                  </v-list-item-content>
                  <v-list-item-title v-if="database.identifier.creators.length > 0" class="mt-2">
                    Creators
                  </v-list-item-title>
                  <v-list-item-content>
                    <span v-for="(person_or_org, i) in database.identifier.creators" :key="`c-${i}`" class="mt-1">
                      <OrcidIcon v-if="person_or_org.orcid" :orcid="person_or_org.orcid" />
                      <v-tooltip
                        top>
                        <template v-slot:activator="{ on, attrs }">
                          <span
                            v-bind="attrs"
                            v-on="on">
                            {{ person_or_org.name }}
                          </span>
                        </template>
                        <span v-if="person_or_org.affiliation">{{ person_or_org.affiliation }}</span>
                      </v-tooltip>
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
                  <v-list-item-title v-if="database.identifier.related.length > 0" class="mt-2">
                    Related Identifiers
                  </v-list-item-title>
                  <v-list-item-content v-if="database.identifier.related.length > 0">
                    <div v-for="(rel, i) in database.identifier.related" :key="`r-${i}`">
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
                  <v-list-item-title v-if="database.identifier.license" class="mt-2">
                    License
                  </v-list-item-title>
                  <v-list-item-content v-if="database.identifier.license">
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <a v-if="database.identifier.license" target="_blank" :href="database.identifier.license.uri">{{ database.identifier.license.identifier }}</a>
                    <span v-if="!database.identifier.license">(none)</span>
                  </v-list-item-content>
                  <v-list-item-title v-if="citation" class="mt-2">
                    Citation
                  </v-list-item-title>
                  <v-list-item-content v-if="citation">
                    <v-row no-gutters>
                      <v-col lg="11" v-text="citation" />
                      <v-col lg="1">
                        <v-select
                          v-model="style"
                          :items="styles"
                          item-text="style"
                          item-value="accept"
                          class="cite-style float-right"
                          dense
                          outlined
                          return-object
                          single-line />
                      </v-col>
                    </v-row>
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
            <v-card-actions>
              <v-btn
                v-if="hasIdentifier"
                small
                color="secondary"
                :loading="metadataLoading"
                @click.stop="download">
                <v-icon left>mdi-code-tags</v-icon> Metadata .xml
              </v-btn>
            </v-card-actions>
          </v-card-text>
        </v-card>
        <v-divider v-if="hasIdentifier" />
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
              <v-btn
                v-if="!hasIdentifier"
                small
                color="primary"
                @click="editDbDialog = true">
                Get Database PID
              </v-btn>
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
import { formatTimestampUTCLabel, formatUser } from '@/utils'
import { decodeJwt } from 'jose'

export default {
  components: {
    DBToolbar,
    Persist,
    OrcidIcon
  },
  data () {
    return {
      loading: false,
      styles: [
        { style: 'APA', accept: 'text/bibliography; style=apa' },
        { style: 'IEEE', accept: 'text/bibliography; style=ieee' },
        { style: 'BibTeX', accept: 'text/bibliography; style=bibtex' }
      ],
      style: null,
      editDbDialog: false,
      citation: null,
      access: {
        type: null,
        user: {
          username: null
        }
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
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    pid () {
      return `${this.baseUrl}/pid/${this.database.identifier.id}`
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
    contact () {
      if (this.database.contact === null || this.database.contact === undefined) {
        return null
      }
      return formatUser(this.database.contact)
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
      return formatUser(this.database.creator)
    },
    creatorVerified () {
      return this.database.creator.email_verified
    },
    hasIdentifier () {
      if (this.database.identifier === null) {
        return false
      }
      return this.database.identifier.id !== null
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
  watch: {
    style (newVal, oldVal) {
      this.loadCitation(newVal)
    }
  },
  mounted () {
    this.loadUser()
    this.loadDatabase()
      .then(() => this.loadCitation(null))
    this.style = this.styles[0]
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
    async loadCitation (style) {
      if (!this.database.identifier.id) {
        return
      }
      this.metadataLoading = true
      try {
        const config = this.config
        config.headers.Accept = 'text/bibliography'
        if (style != null) {
          config.headers.Accept = `${config.headers.Accept}; style=${style}`
        }
        const res = await this.$axios.get(`/api/pid/${this.database.identifier.id}`, config)
        this.citation = res.data
        console.debug('citation', this.citation)
      } catch (err) {
        console.error('Could not cite identifier', err)
        this.$toast.error('Could not cite identifier')
        this.error = true
      }
      this.metadataLoading = false
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
    }
  }
}
</script>
<style>
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
</style>
