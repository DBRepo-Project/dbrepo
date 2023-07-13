<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="backTo">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title v-if="title" v-text="title" />
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canPersistQuery" :loading="loadingSave" class="mb-1" @click.stop="save">
          <v-icon left>mdi-content-save-outline</v-icon> Save
        </v-btn>
        <v-btn v-if="query.is_persisted && !query.identifier && canWrite" class="mb-1 ml-2" color="primary" :disabled="!executionUTC" :to="`/database/${$route.params.database_id}/query/${$route.params.query_id}/persist`">
          <v-icon left>mdi-content-save-outline</v-icon> Get PID
        </v-btn>
        <v-btn v-if="result_visibility && query.result_number" class="mb-1 ml-2" :loading="downloadLoading" @click.stop="downloadSubset">
          <v-icon left>mdi-download</v-icon> Data .csv
        </v-btn>
        <DownloadButton v-if="query.identifier" :pid="query.identifier.id" class="mb-1 ml-2">
          <v-icon left>mdi-code-tags</v-icon> Identifier .xml
        </DownloadButton>
      </v-toolbar-title>
    </v-toolbar>
    <Summary v-if="showIdentifierCard" :identifier="identifier" />
    <v-divider v-if="query && query.identifier" />
    <v-card flat tile>
      <v-card-title>
        Subset Information
      </v-card-title>
      <v-card-text>
        <v-alert
          v-if="canPersistQuery"
          border="left"
          color="info">
          Query is not yet saved in the query store, <a @click="save">save</a> it to view it later.
        </v-alert>
        <v-alert
          v-if="isAuthorizationError"
          border="left"
          color="error">
          You do not have permission to view this subset.
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
              <div v-if="database && database.identifier && database.identifier.license">
                <v-list-item-title class="mt-2">
                  Database License
                </v-list-item-title>
                <v-list-item-content>
                  <a :href="database.identifier.license.uri">{{ database.identifier.license.identifier }}</a>
                </v-list-item-content>
              </div>
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
    <v-divider />
    <QueryResults
      id="query-results"
      ref="queryResults"
      v-model="query.id"
      type="query"
      class="mt-0 mb-0" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import Summary from '@/components/identifier/Summary.vue'
import DownloadButton from '@/components/identifier/DownloadButton.vue'
import { formatTimestampUTCLabel, formatDateUTC } from '@/utils'
import QueryService from '@/api/query.service'
import UserUtils from '@/api/user.utils'

export default {
  name: 'QueryShow',
  components: {
    DownloadButton,
    Summary
  },
  data () {
    return {
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Queries', to: `/database/${this.$route.params.database_id}/query`, activeClass: '' },
        { text: `${this.$route.params.query_id}`, to: `/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, activeClass: '' }
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
      downloadLoading: false,
      isAuthorizationError: false,
      error: false,
      promises: []
    }
  },
  computed: {
    baseUrl () {
      return location.protocol + '//' + location.host
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
    identifier () {
      if (!this.query) {
        return null
      }
      return this.query.identifier
    },
    hasIdentifier () {
      return this.query.identifier !== null
    },
    title () {
      if (!this.hasIdentifier) {
        return null
      }
      const enTitle = this.query.identifier.titles.filter(t => t.language).filter(t => t.language === 'en')
      if (enTitle.length !== 1) {
        return this.query.identifier.titles[0].title
      }
      return enTitle[0].title
    },
    result_hash () {
      if (!this.query.result_hash) {
        return '(none)'
      }
      return this.query.result_hash
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
    canPersistQuery () {
      if (this.loadingQuery || !this.query || this.query.is_persisted) {
        return false
      }
      return UserUtils.hasReadAccess(this.access)
    },
    publisher () {
      if (this.database.publisher === null) {
        return 'NA'
      }
      return this.database.publisher
    },
    isOwner () {
      if (!this.query || !this.user) {
        return false
      }
      return this.query.creator.username === this.user.username
    },
    backTo () {
      return `/database/${this.$route.params.database_id}/query`
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
      return this.access.type === 'write_own' || this.access.type === 'write_all'
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
    downloadSubset () {
      this.downloadLoading = true
      QueryService.exportSubset(this.$route.params.database_id, this.$route.params.query_id)
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
        QueryService.findOne(this.$route.params.database_id, this.$route.params.query_id)
          .then((query) => {
            this.query = query
            resolve(query)
          })
          .catch((error) => {
            if (error.response.status === 405) {
              this.isAuthorizationError = true
            }
            reject(error)
          })
          .finally(() => {
            this.loadingQuery = false
          })
      })
    },
    save () {
      this.loadingSave = true
      QueryService.persist(this.$route.params.database_id, this.$route.params.query_id)
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
