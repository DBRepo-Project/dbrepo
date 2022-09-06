<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-skeleton-loader v-if="loadingIdentifier" type="text" class="skeleton-small" />
        <span v-if="!loadingIdentifier">{{ identifier.title }}</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="token && !identifier.id && !loadingIdentifier && is_owner" class="mb-1 mr-2" color="primary" :disabled="error || erroneous || !executionUTC" @click.stop="openDialog()">
          <v-icon left>mdi-content-save-outline</v-icon> Save
        </v-btn>
        <v-btn v-if="result_visibility" class="mb-1" :disabled="error" :loading="downloadLoading" @click.stop="download">
          <v-icon left>mdi-download</v-icon> Data .csv
        </v-btn>
        <v-btn
          v-if="identifier.id"
          :disabled="error"
          color="secondary"
          class="ml-2"
          :loading="metadataLoading"
          @click.stop="metadata">
          <v-icon left>mdi-code-tags</v-icon> Metadata .xml
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat tile>
      <v-card-title>
        Subset Information
      </v-card-title>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-icon>
              <v-icon :color="database_visibility ? 'success' : 'error'">mdi-database-outline</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Database Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="loadingDatabase" type="text" class="skeleton-small" />
                <span v-if="!loadingDatabase && database_visibility">Public</span>
                <span v-if="!loadingDatabase && !database_visibility">Private</span>
              </v-list-item-content>
              <v-list-item-title v-if="database.name" class="mt-2">
                Database Name
              </v-list-item-title>
              <v-list-item-content v-if="database.name">
                <v-skeleton-loader v-if="loadingDatabase" type="text" class="skeleton-small" />
                <span v-if="!loadingDatabase">{{ database.name }}</span>
              </v-list-item-content>
              <v-list-item-title v-if="database.publisher" class="mt-2">
                Database Publisher
              </v-list-item-title>
              <v-list-item-content v-if="database.publisher">
                <v-skeleton-loader v-if="loadingDatabase" type="text" class="skeleton-small" />
                <span v-if="!loadingDatabase">{{ database.publisher }}</span>
              </v-list-item-content>
              <v-list-item-title v-if="database.license" class="mt-2">
                Database License
              </v-list-item-title>
              <v-list-item-content v-if="database.license">
                <v-skeleton-loader v-if="loadingDatabase" type="text" class="skeleton-xsmall" />
                <a v-if="!loadingDatabase" :href="database.license.uri">{{ database.license.identifier }}</a>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="identifier.id">
            <v-list-item-icon>
              <v-icon>mdi-lock-clock</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Persistent Identifier
              </v-list-item-title>
              <v-list-item-content>
                <a :href="`${baseUrl}/pid/${identifier.id}`">{{ baseUrl }}/pid/{{ identifier.id }}</a>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Title
              </v-list-item-title>
              <v-list-item-content>
                {{ identifier.title }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Description
              </v-list-item-title>
              <v-list-item-content>
                {{ identifier.description }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Creators
              </v-list-item-title>
              <v-list-item-content>
                <span v-for="(person_or_org, i) in identifier.creators" :key="`c-${i}`" class="mt-1">
                  <OrcidIcon v-if="person_or_org.orcid" :orcid="person_or_org.orcid" />
                  {{ person_or_org.name }} <sup v-if="person_or_org.affiliation">{{ person_or_org.affiliation }}</sup>
                </span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Publication Date
              </v-list-item-title>
              <v-list-item-content>
                {{ publication }}
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
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-text-short</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Query Statement
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="loadingQuery" type="text" class="skeleton-large" />
                <pre v-if="!loadingQuery">{{ query_statement }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Hash
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="loadingQuery" type="text" class="skeleton-medium" />
                <pre v-if="!loadingQuery">{{ query_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Creator
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="loadingQuery" type="text" class="skeleton-small" />
                <span v-if="!loadingQuery">
                  {{ creator }} <sup>
                    <v-icon v-if="database.creator.email_verified" small color="primary">mdi-check-decagram</v-icon>
                  </sup>
                </span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Creation
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="loadingQuery" type="text" class="skeleton-small" />
                <span v-if="!loadingQuery">{{ executionUTC }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon :color="result_visibility_icon ? 'success' : 'error'">{{ result_icon }}</v-icon>
            </v-list-item-icon>
            <v-list-item-content v-if="!erroneous">
              <v-list-item-title>
                Result Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="metadataLoading" type="text" class="skeleton-xsmall" />
                <span v-if="!metadataLoading">{{ result_visibility_icon ? 'Public' : 'Private' }}</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Result Hash
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="metadataLoading" type="text" class="skeleton-medium" />
                <pre v-if="!metadataLoading">{{ result_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Result Number
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="metadataLoading" type="text" class="skeleton-xsmall" />
                <span v-if="!metadataLoading">{{ result_number }}</span>
              </v-list-item-content>
            </v-list-item-content>
            <v-list-item-content v-if="erroneous">
              <v-list-item-title>
                Result Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-alert
                  v-if="!error && !loadingQuery && erroneous"
                  border="left"
                  color="error">
                  This query failed to execute and did not produce a subset.
                </v-alert>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <QueryResults
      v-if="!erroneous"
      id="query-results"
      ref="queryResults"
      v-model="query.id"
      :query-id="query.id"
      class="mt-0 mb-0" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="persistQueryDialog"
      persistent
      max-width="860">
      <PersistQuery @close="closeDialog" />
    </v-dialog>
  </div>
</template>
<script>
import PersistQuery from '@/components/dialogs/PersistQuery'
import OrcidIcon from '@/components/icons/OrcidIcon'
import { formatTimestampUTCLabel, formatDateUTC } from '@/utils'
import { decodeJwt } from 'jose'

export default {
  name: 'QueryShow',
  components: {
    PersistQuery,
    OrcidIcon
  },
  data () {
    return {
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Queries', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query`, activeClass: '' },
        { text: `${this.$route.params.query_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, activeClass: '' }
      ],
      query: {
        id: parseInt(this.$route.params.query_id),
        database_id: null,
        query: null,
        query_hash: null,
        result_hash: null,
        result_number: null,
        execution: null,
        created: null,
        creator: {
          username: null,
          firstname: null,
          lastname: null
        }
      },
      user: {
        username: null
      },
      identifier: {
        id: null,
        dbid: null,
        qid: null,
        title: null,
        description: null,
        visibility: null,
        query: null,
        query_normalized: null,
        query_hash: null,
        result_number: null,
        execution: null,
        publication_year: null,
        publication_month: null,
        publication_day: null,
        related: [],
        doi: null,
        creators: []
      },
      database: {
        id: null,
        name: null,
        is_public: null,
        publisher: null,
        creator: {
          username: null,
          email_verified: false
        },
        license: {
          identifier: null,
          uri: null
        }
      },
      persistQueryExists: false,
      persistQueryDialog: false,
      loadingDatabase: false,
      loadingIdentifier: false,
      loadingQuery: true,
      metadataLoading: false,
      downloadLoading: false,
      error: false,
      promises: []
    }
  },
  computed: {
    result_icon () {
      return this.erroneous && !this.loadingQuery ? 'mdi-flash' : 'mdi-table'
    },
    baseUrl () {
      return location.protocol + '//' + location.host
    },
    loadingColor () {
      return this.error ? 'red' : 'primary'
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
    query_statement () {
      return this.query.query ? this.query.query : this.identifier.query
    },
    publisher () {
      if (this.database.publisher === null) {
        return 'NA'
      }
      return this.database.publisher
    },
    username () {
      return this.$store.state.user && this.$store.state.user.username
    },
    database_visibility () {
      return this.database.is_public
    },
    is_owner () {
      return this.token && this.query.creator.username === this.user.username
    },
    result_visibility () {
      if (this.erroneous) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (this.query.creator.username === this.username) {
        return true
      }
      return this.identifier.visibility === 'everyone'
    },
    result_visibility_icon () {
      if (this.erroneous) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      return this.identifier.visibility === 'everyone'
    },
    publication () {
      if (this.identifier.publication_year && !this.identifier.publication_month && !this.identifier.publication_day) {
        return this.identifier.publication_year
      } else if (this.identifier.publication_year && this.identifier.publication_month && this.identifier.publication_day) {
        return formatDateUTC(this.identifier.publication_year + '-' + this.identifier.publication_month + '-' + this.identifier.publication_day)
      } else {
        return null
      }
    },
    query_hash () {
      return 'sha256:' + (this.identifier.id ? this.identifier.query_hash : this.query.query_hash)
    },
    result_number () {
      return this.identifier.id ? this.identifier.result_number : this.query.result_number
    },
    result_hash () {
      return 'sha256:' + (this.identifier.id ? this.identifier.result_hash : this.query.result_hash)
    },
    executionUTC () {
      return this.identifier.id ? formatTimestampUTCLabel(this.identifier.execution) : formatTimestampUTCLabel(this.query.execution)
    },
    creator () {
      if (this.query.creator.username === null) {
        return null
      }
      if (this.query.creator.firstname === null || this.query.creator.lastname === null) {
        return this.query.creator.username
      }
      return this.query.creator.firstname + ' ' + this.query.creator.lastname
    },
    creators () {
      return this.identifier.id ? this.identifier.creators : null
    },
    erroneous () {
      return !this.query.result_hash
    }
  },
  mounted () {
    this.loadUser()
    this.loadDatabase()
      .then(() => this.loadQuery())
      .then(() => this.loadMetadata())
  },
  methods: {
    async metadata () {
      this.metadataLoading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/identifier/${this.identifier.id}`, this.config)
        console.debug('identifier result', res)
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'metadata.xml')
        document.body.appendChild(link)
        link.click()
      } catch (err) {
        console.error('Could not export metadata', err)
        this.$toast.error('Could not export metadata')
        this.error = true
      }
      this.metadataLoading = false
    },
    async download () {
      this.downloadLoading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}/export`, this.config)
        console.debug('export query result', res)
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'query.csv')
        document.body.appendChild(link)
        link.click()
      } catch (err) {
        console.error('Could not export query result', err)
        this.$toast.error('Could not export query result')
        this.error = true
      }
      this.downloadLoading = false
    },
    async loadQuery () {
      this.loadingQuery = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, this.config)
        console.debug('query', res.data)
        this.query = res.data
      } catch (err) {
        if (err.response.status !== 401 && err.response.status !== 405) {
          console.error('Could not load query', err)
          this.$toast.error('Could not load query')
        }
        this.error = true
      }
      this.loadingQuery = false
    },
    async loadDatabase () {
      this.loadingDatabase = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        console.debug('database', res.data)
        this.database = res.data
      } catch (err) {
        this.error = true
      }
      this.loadingDatabase = false
    },
    async loadMetadata () {
      if (!this.query.id) {
        return
      }
      this.loadingIdentifier = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/identifier?qid=${this.$route.params.query_id}`, this.config)
        this.identifier = res.data[0]
        console.debug('identifier', res.data[0])
      } catch (err) {
        if (err.response.status !== 404) {
          this.error = true
          console.error('Could not load identifier', err)
          this.$toast.error('Could not load identifier')
        }
      }
      this.loadingIdentifier = false
    },
    openDialog () {
      this.persistQueryDialog = true
    },
    closeDialog (event) {
      this.persistQueryDialog = false
      if (event.action === 'persisted') {
        this.loadMetadata()
      }
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
pre {
  white-space: break-spaces;
}
.v-card__text {
  font-size: initial;
}
.skeleton-large .v-skeleton-loader__text {
  width: 400px;
}
.skeleton-medium .v-skeleton-loader__text {
  width: 200px;
}
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
.skeleton-xsmall .v-skeleton-loader__text {
  width: 50px;
}
</style>
