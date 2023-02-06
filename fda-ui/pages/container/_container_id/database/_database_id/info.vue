<template>
  <div v-if="database">
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="!loadingCitation && (isDataSteward || hasIdentifier || (!hasIdentifier && isCreator && isResearcher))" flat tile>
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
                  <v-skeleton-loader v-if="loadingCitation" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
          <v-card-text>
            <v-card-actions>
              <v-btn
                v-if="!hasIdentifier && (isDataSteward || (!hasIdentifier && isCreator && isResearcher))"
                small
                color="primary"
                @click="editDbDialog = true">
                Get Database PID
              </v-btn>
              <!--                v-if="isDataSteward && hasIdentifier"-->
              <v-btn
                v-if="false"
                small
                :loading="loadingDelete"
                color="error"
                @click="deleteIdentifier">
                Delete Database PID
              </v-btn>
            </v-card-actions>
          </v-card-text>
        </v-card>
        <v-divider v-if="!loadingCitation && (hasIdentifier || (isCreator && isResearcher) || isDataSteward)" />
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
      </v-tab-item>
    </v-tabs-items>
    <v-dialog
      v-model="editDbDialog"
      persistent
      max-width="860">
      <Persist type="database" :database="database" @close="closeDialog" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DBToolbar from '@/components/DBToolbar'
import Persist from '@/components/dialogs/Persist'
import OrcidIcon from '@/components/icons/OrcidIcon'
import Citation from '@/components/identifier/Citation'
import { formatTimestampUTCLabel, formatUser, isDataSteward, isResearcher } from '@/utils'

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
      loadingCitation: false,
      loadingDelete: false,
      editDbDialog: false,
      citation: null,
      metadataLoading: false,
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
    identifier () {
      if (this.database) {
        return this.$store.state.database.identifier
      }
      return null
    },
    access () {
      return this.$store.state.access
    },
    database () {
      return this.$store.state.database
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
    isResearcher () {
      return isResearcher(this.user)
    },
    isDataSteward () {
      return isDataSteward(this.user)
    },
    pid () {
      return `${this.baseUrl}/pid/${this.database.identifier.id}`
    },
    createdUTC () {
      return formatTimestampUTCLabel(this.database.created)
    },
    isCreator () {
      if (!this.database) {
        return false
      }
      if (!this.database.creator.username || !this.user || !this.user.username) {
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
      if ('identifier' in this.database && this.database.identifier) {
        return 'id' in this.database.identifier
      }
      return false
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
  mounted () {
    this.loadingCitation = true
    this.loadCitation()
  },
  methods: {
    async closeDialog (event) {
      if (event.action === 'persisted') {
        await this.loadDatabase()
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
    async loadCitation () {
      if (!this.database || !this.database.identifier) {
        this.loadingCitation = false
        return
      }
      try {
        const res = await this.$axios.get(`/api/pid/${this.database.identifier.id}`, this.config)
        this.citation = res.data
        this.citation.affiliations = []
        this.citation.creators.forEach((personOrOrg) => {
          const affiliationId = this.identifier.affiliations.indexOf(personOrOrg.affiliation)
          if (affiliationId === -1) {
            this.citation.affiliations.push(personOrOrg.affiliation)
            personOrOrg.affiliation_id = this.citation.affiliations.indexOf(personOrOrg.affiliation) + 1
          } else {
            personOrOrg.affiliation_id = affiliationId + 1
          }
        })
        console.debug('citation', this.citation)
      } catch (err) {
        console.error('Failed to load citation', err)
        this.$toast.error('Failed to load citation')
      }
      this.loadingCitation = false
    },
    async deleteIdentifier () {
      if (!this.database.identifier.id) {
        return
      }
      this.loadingDelete = true
      try {
        await this.$axios.delete(`/api/identifier/${this.database.identifier.id}`, this.config)
        console.info('Deleted identifier with id ', this.database.identifier.id)
        this.$toast.success('Successfully deleted identifier with id ' + this.database.identifier.id)
        await this.loadDatabase()
      } catch (error) {
        const { message } = error.response
        console.error('Failed to delete identifier', error)
        this.$toast.error('Failed to delete identifier: ' + message)
      }
      this.loadingDelete = false
    },
    async loadDatabase () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.$store.commit('SET_DATABASE', res.data)
        console.debug('database', this.database)
      } catch (err) {
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
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
