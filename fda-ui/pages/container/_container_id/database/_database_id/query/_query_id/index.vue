<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>{{ identifier.title }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="!identifier.id && !loading" color="secondary" class="mr-2" :disabled="!execution || !token" @click.stop="openDialog()">
          <v-icon left>mdi-fingerprint</v-icon> Persist
        </v-btn>
        <v-btn v-if="result_visibility" color="primary" :loading="exportLoading" @click.stop="download">
          <v-icon left>mdi-download</v-icon> Download
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card v-if="!loading" class="pb-2" flat>
      <v-card-title>
        Query Information
      </v-card-title>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-icon>
              <v-icon :color="database.is_public ? 'success' : 'error'">mdi-database</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Database Visibility
              </v-list-item-title>
              <v-list-item-content>
                {{ database.is_public ? 'Public' : 'Private' }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Database Publisher
              </v-list-item-title>
              <v-list-item-content>
                {{ publisher }}
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
                <a :href="`https://dbrepo.ossdip.at/pid/${identifier.id}`">https://dbrepo.ossdip.at/pid/{{ identifier.id }}</a>
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
                <span v-for="(creator, i) in identifier.creators" :key="i" class="mt-1">
                  <OrcidIcon v-if="creator.orcid" :orcid="creator.orcid" />
                  {{ creator.name }} <sup v-if="creator.affiliation">{{ creator.affiliation }}</sup>
                </span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Publication Date
              </v-list-item-title>
              <v-list-item-content>
                {{ identifier.publication_year }}
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
                <pre>{{ query.query }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Query Hash
              </v-list-item-title>
              <v-list-item-content>
                <pre>{{ query_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Query Creator
              </v-list-item-title>
              <v-list-item-content>
                {{ creator }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Query Execution
              </v-list-item-title>
              <v-list-item-content>
                {{ execution }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Query Creation
              </v-list-item-title>
              <v-list-item-content>
                {{ query_creation }}
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon :color="result_visibility_icon ? 'success' : 'error'">mdi-table</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Result Visibility
              </v-list-item-title>
              <v-list-item-content>
                {{ result_visibility_icon ? 'Public' : 'Private' }}
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Result Hash
              </v-list-item-title>
              <v-list-item-content>
                <pre>{{ result_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Result Number
              </v-list-item-title>
              <v-list-item-content>
                {{ result_number }}
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
      <QueryResults ref="queryResults" v-model="query.id" :query-id="query.id" class="mt-0 ml-4 mr-4 mb-2" />
    </v-card>
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
import { format } from 'date-fns'
import PersistQuery from '@/components/dialogs/PersistQuery'
import OrcidIcon from '@/components/icons/OrcidIcon'

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
        id: this.$route.params.query_id,
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
        doi: null,
        creators: []
      },
      database: {
        id: null,
        name: null,
        is_public: null,
        publisher: null,
        creator: {
          username: null
        }
      },
      persistQueryExists: false,
      persistQueryDialog: false,
      loading: true,
      exportLoading: false,
      error: false,
      promises: []
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    loadingColor () {
      return this.error ? 'red' : 'primary'
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
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
    query_visibility () {
      if (this.database.is_public) {
        return true
      }
      return this.database.creator.username === this.username
    },
    result_everyone () {
      return this.database.is_public || this.identifier.visibility === 'EVERYONE'
    },
    result_visibility () {
      if (this.database.is_public) {
        return true
      }
      if (this.query.creator.username === this.username) {
        return true
      }
      return this.identifier.visibility === 'EVERYONE'
    },
    result_visibility_icon () {
      if (this.database.is_public) {
        return true
      }
      return this.identifier.visibility === 'EVERYONE'
    },
    statement () {
      return this.identifier.id ? this.identifier.query : this.query.query
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
    execution () {
      return this.identifier.id ? this.formatDate(this.identifier.execution) : this.formatDate(this.query.execution)
    },
    query_creation () {
      return this.formatDate(this.query.created)
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
    }
  },
  mounted () {
    this.loadDatabase()
      .then(() => this.loadQuery())
      .then(() => this.loadMetadata())
  },
  methods: {
    formatDate (d) {
      return format(new Date(d), 'dd.MM.yyyy HH:mm:ss')
    },
    async download () {
      this.exportLoading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}/export`, {
          headers: { Authorization: `Bearer ${this.token}` },
          responseType: 'text'
        })
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
      this.exportLoading = false
    },
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        console.debug('database', res.data)
        this.database = res.data
      } catch (err) {
        if (err.response.status !== 401) {
          console.error('Could not load database', err)
          this.$toast.error('Could not load database')
        }
        this.error = true
      }
      this.loading = false
    },
    async loadQuery () {
      if (!this.query_visibility) {
        return
      }
      this.loading = true
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
      this.loading = false
    },
    async loadMetadata () {
      if (!this.query.id) {
        return
      }
      this.loading = true
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
      this.loading = false
    },
    openDialog () {
      this.persistQueryDialog = true
    },
    closeDialog (event) {
      this.persistQueryDialog = false
      if (event.action === 'persisted') {
        this.loadMetadata()
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
</style>
