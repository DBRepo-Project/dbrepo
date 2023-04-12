<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="backTo">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        <span v-if="query.identifier">{{ query.identifier.title }}</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="!query.is_persisted && canWrite" :loading="loadingSave" class="mb-1 mr-2" @click.stop="save">
          <v-icon left>mdi-content-save-outline</v-icon> Save
        </v-btn>
        <v-btn v-if="query.is_persisted && !query.identifier && canWrite" class="mb-1 mr-2" color="primary" :disabled="!executionUTC" @click.stop="openDialog()">
          <v-icon left>mdi-content-save-outline</v-icon> Get PID
        </v-btn>
        <v-btn v-if="result_visibility && !query.identifier && query.result_number" class="mb-1" :loading="downloadLoading" @click.stop="downloadSubset">
          <v-icon left>mdi-download</v-icon> Data .csv
        </v-btn>
        <v-btn v-if="result_visibility && query.identifier && query.result_number" class="mb-1" :loading="downloadLoading" @click.stop="downloadMetadata('text/csv')">
          <v-icon left>mdi-download</v-icon> Data .csv
        </v-btn>
        <v-btn
          v-if="query.identifier"
          color="secondary"
          class="ml-2"
          :loading="metadataLoading"
          @click.stop="downloadMetadata('text/xml')">
          <v-icon left>mdi-code-tags</v-icon> Metadata .xml
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat tile>
      <v-card-title>
        Subset Information
      </v-card-title>
      <v-card-text>
        <v-alert
          v-if="!loadingQuery && !query.is_persisted && canWrite"
          border="left"
          color="info">
          Query is not yet saved in the query store, <a @click="save">save</a> it to view it later.
        </v-alert>
        <v-list dense>
          <v-list-item>
            <v-list-item-icon>
              <v-icon v-if="!database">mdi-database-outline</v-icon>
              <v-icon v-if="database" :color="database.is_public ? 'success' : 'error'">mdi-database-outline</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Database Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-small" />
                <span v-if="database">{{ database.is_public ? 'Public' : 'Private' }}</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Database Name
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-small" />
                <span v-if="database">{{ database.name }}</span>
              </v-list-item-content>
              <div v-if="database && database.identifier">
                <v-list-item-title class="mt-2">
                  Database License
                </v-list-item-title>
                <v-list-item-content>
                  <a :href="database.identifier.license.uri">{{ database.identifier.license.identifier }}</a>
                </v-list-item-content>
              </div>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="query && query.identifier">
            <v-list-item-icon>
              <v-icon>mdi-lock-clock</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Persistent Identifier
              </v-list-item-title>
              <v-list-item-content>
                <a :href="`${baseUrl}/pid/${query.identifier.id}`">{{ baseUrl }}/pid/{{ query.identifier.id }}</a>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Title
              </v-list-item-title>
              <v-list-item-content>
                {{ query.identifier.title }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Description
              </v-list-item-title>
              <v-list-item-content>
                {{ query.identifier.description }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Publisher
              </v-list-item-title>
              <v-list-item-content>
                {{ query.identifier.publisher }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Publication Date
              </v-list-item-title>
              <v-list-item-content>
                {{ publication }}
              </v-list-item-content>
              <v-list-item-title v-if="query.identifier.related.length > 0" class="mt-2">
                Related Identifiers
              </v-list-item-title>
              <v-list-item-content v-if="query.identifier.related.length > 0">
                <div v-for="(rel, i) in query.identifier.related" :key="`r-${i}`">
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
              <Citation :pid="pid" />
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
                <v-skeleton-loader v-if="!query" type="text, text" />
                <pre v-if="query">{{ query.query }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Hash
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!query" type="text" class="skeleton-large" />
                <pre v-if="query">sha256:{{ query.query_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Creation
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!executionUTC" type="text" class="skeleton-small" />
                <span v-if="executionUTC">{{ executionUTC }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon v-if="!query">mdi-table</v-icon>
              <v-icon v-if="database && !query.identifier" :color="database.is_public ? 'success' : 'error'">mdi-table</v-icon>
              <v-icon v-if="query && query.identifier" :color="query.identifier.visibility === 'everyone' ? 'success' : 'error'">mdi-table</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Result Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!query" type="text" class="skeleton-xsmall" />
                <span v-if="database && !query.identifier">{{ database.is_public ? 'Public' : 'Private' }}</span>
                <span v-if="query && query.identifier">{{ query.identifier.visibility === 'everyone' ? 'Public' : 'Private' }}</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Result Hash
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!query" type="text" class="skeleton-large" />
                <pre v-if="query">{{ result_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Result Number
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!query" type="text" class="skeleton-xsmall" />
                <span v-if="query">{{ query.result_number }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <QueryResults
      id="query-results"
      ref="queryResults"
      v-model="query.id"
      type="query"
      class="mt-0 mb-0" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="persistQueryDialog"
      persistent
      max-width="860">
      <Persist @close="closeDialog" />
    </v-dialog>
  </div>
</template>
<script>
import Persist from '@/components/dialogs/Persist'
import Citation from '@/components/identifier/Citation'
import { formatTimestampUTCLabel, formatDateUTC } from '@/utils'
import QueryService from '@/api/query.service'

export default {
  name: 'QueryShow',
  components: {
    Persist,
    Citation
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
        is_persisted: null,
        identifier: null,
        creator: {
          username: null,
          firstname: null,
          lastname: null
        }
      },
      loadingSave: false,
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
    baseUrl () {
      return location.protocol + '//' + location.host
    },
    loadingColor () {
      return this.error ? 'error' : 'primary'
    },
    pid () {
      if (this.query.identifier) {
        return this.query.identifier.id
      }
      return 0
    },
    token () {
      return this.$store.state.token
    },
    database () {
      return this.$store.state.database
    },
    access () {
      return this.$store.state.access
    },
    user () {
      return this.$store.state.user
    },
    title () {
      return null
    },
    result_hash () {
      if (!this.query.result_hash) {
        return '(none)'
      }
      return this.query.result_hash
    },
    config () {
      if (this.token === null) {
        return {
          headers: {},
          progress: false
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` },
        progress: false
      }
    },
    silentConfig () {
      return {
        headers: this.config.headers,
        progress: false
      }
    },
    publisher () {
      if (this.database.publisher === null) {
        return 'NA'
      }
      return this.database.publisher
    },
    backTo () {
      return `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query`
    },
    result_visibility () {
      if (!this.database || this.database.is_public === null) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (this.query.creator.username === this.username) {
        return true
      }
      if (!this.query.identifier) {
        return false
      }
      return this.query.identifier.visibility === 'everyone'
    },
    canWrite () {
      if (!this.access || !this.access.type) {
        return false
      }
      if (this.access.type === 'write_own' || this.access.type === 'write_all') {
        return true
      }
      return false
    },
    publication () {
      if (this.query.identifier.publication_year && !this.query.identifier.publication_month && !this.query.identifier.publication_day) {
        return this.query.identifier.publication_year
      } else if (this.query.identifier.publication_year && this.query.identifier.publication_month && this.query.identifier.publication_day) {
        return formatDateUTC(this.query.identifier.publication_year + '-' + this.query.identifier.publication_month + '-' + this.query.identifier.publication_day)
      } else {
        return null
      }
    },
    executionUTC () {
      return formatTimestampUTCLabel(this.query.created)
    }
  },
  mounted () {
    this.loadQuery()
      .then(() => this.loadResult())
  },
  methods: {
    loadResult () {
      this.$refs.queryResults.reExecute(this.query.id)
      this.$refs.queryResults.reExecuteCount(this.query.id)
    },
    downloadMetadata (mime) {
      if (mime === 'text/csv') {
        this.downloadLoading = true
      } else if (mime === 'text/xml') {
        this.metadataLoading = true
      }
      QueryService.exportMetadata(this.query.identifier.id, mime)
        .then((metadata) => {
          const url = window.URL.createObjectURL(new Blob([metadata]))
          const link = document.createElement('a')
          link.href = url
          if (mime === 'text/csv') {
            link.setAttribute('download', 'subset.csv')
          } else if (mime === 'text/xml') {
            link.setAttribute('download', 'identifier.xml')
          }
          document.body.appendChild(link)
          link.click()
        })
        .finally(() => {
          this.downloadLoading = false
          this.metadataLoading = false
        })
    },
    downloadSubset () {
      this.downloadLoading = true
      QueryService.exportSubset(this.$route.params.container_id, this.$route.params.database_id, this.$route.params.query_id)
        .then((data) => {
          const url = window.URL.createObjectURL(new Blob([data]))
          const link = document.createElement('a')
          link.href = url
          link.setAttribute('download', 'subset.csv')
          document.body.appendChild(link)
          link.click()
        })
        .finally(() => {
          this.downloadLoading = false
        })
    },
    loadQuery () {
      this.loadingQuery = true
      return new Promise((resolve, reject) => {
        QueryService.findOne(this.$route.params.container_id, this.$route.params.database_id, this.$route.params.query_id)
          .then((query) => {
            this.query = query
            resolve(query)
          })
          .catch(error => reject(error))
          .finally(() => {
            this.loadingQuery = false
          })
      })
    },
    save () {
      this.loadingSave = true
      QueryService.persist(this.$route.params.container_id, this.$route.params.database_id, this.$route.params.query_id)
        .then((query) => {
          this.query = query
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    openDialog () {
      this.persistQueryDialog = true
    },
    closeDialog (event) {
      this.persistQueryDialog = false
      if (event.action === 'persisted') {
        this.loadQuery()
      }
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
.skeleton-large > div {
  width: 400px !important;
}
.skeleton-medium > div {
  width: 200px !important;
}
.skeleton-small > div {
  width: 100px !important;
}
.skeleton-xsmall > div {
  width: 50px !important;
}
.v-data-table {
  border-radius: 0;
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
