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
        <p v-if="database.publisher">
          <strong>Database</strong>
        </p>
        <div v-if="database.publisher">
          <p>
            Publisher: <code>{{ database.publisher }}</code>
          </p>
        </div>
        <p>
          <strong>Query</strong>
        </p>
        <div>
          <p>
            Visibility
            <span v-if="query_visibility"><v-icon small color="teal" title="Public">mdi-eye</v-icon></span>
            <span v-if="!query_visibility"><v-icon small color="red accent-3" title="Private">mdi-eye-off</v-icon></span>
          </p>
          <p v-if="identifier.id">
            Persistent Identifier: <code>https://dbrepo.ossdip.at/pid/{{ identifier.id }}</code>
          </p>
          <p v-if="identifier.publication_year">
            Publication Year: <code>{{ identifier.publication_year }}</code>
          </p>
          <p v-if="creator">
            Owner: <code>{{ creator.username }}</code><span v-if="!creator.username">(empty)</span>
          </p>
          <p>Statement</p>
          <v-alert
            border="left"
            color="code">
            <pre>{{ statement }}</pre>
          </v-alert>
          <p v-if="query_hash">
            Hash: <code>{{ query_hash }}</code>
          </p>
        </div>
        <p class="mt-2">
          <strong>Description</strong>
        </p>
        <div>
          <p v-if="!identifier.description">
            (empty) &#8212; <a href="#" @click.stop="openDialog()">modify</a>
          </p>
          <p v-if="identifier.description">{{ identifier.description }}</p>
        </div>
        <p class="mt-2">
          <strong>Creator(s)</strong>
        </p>
        <p v-if="identifier.creators.length === 0">
          (empty) &#8212; <a href="#" @click.stop="openDialog()">modify</a>
        </p>
        <p v-for="(creator,i) in identifier.creators" :key="i">
          <OrcidIcon v-if="creator.orcid" :orcid="creator.orcid" />
          <span>{{ creator.name }}</span>
          <sup v-if="creator.affiliation">{{ creator.affiliation }}</sup>
        </p>
        <p class="mt-2">
          <strong>Result</strong>
        </p>
        <p>
          Visiblity
          <span v-if="result_everyone"><v-icon small color="teal" title="Public">mdi-eye</v-icon></span>
          <span v-if="!result_everyone"><v-icon small color="red accent-3" title="Private">mdi-eye-off</v-icon></span>
        </p>
        <p v-if="result_hash">
          Hash: <code v-if="result_hash">{{ result_hash }}</code>
        </p>
        <p>
          Rows: <code v-if="result_number">{{ result_number }}</code><span v-if="!result_number">(empty)</span>
        </p>
        <p v-if="execution">
          Executed: <code>{{ execution }}</code>
        </p>
        <QueryResults ref="queryResults" v-model="query.id" :query-id="query.id" class="mt-0" />
      </v-card-text>
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
          username: null
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
    creator () {
      return null
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
</style>
