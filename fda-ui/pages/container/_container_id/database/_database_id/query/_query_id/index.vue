<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>{{ identifier.title }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="!identifier.id" color="accent" class="mr-2" :disabled="!execution || !token" @click.stop="openDialog()">
          <v-icon left>mdi-fingerprint</v-icon> Persist
        </v-btn>
        <v-btn v-if="false" color="primary" :disabled="!token" @click.stop="reExecute">
          <v-icon left>mdi-run</v-icon> Re-Execute
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card v-if="!loading" class="pb-2" flat>
      <v-card-title>
        Query Information
      </v-card-title>
      <v-card-subtitle>
        <span v-if="query.created != null">
          Created {{ formatDate(query.created) }}
        </span>
        <span v-if="execution == null">
          Query was never executed
        </span>
      </v-card-subtitle>
      <v-card-text>
        <p>
          <strong>Query</strong>
        </p>
        <div>
          <p>
            Persistent Identifier: <code v-if="identifier.id">https://dbrepo.ossdip.at/pid/{{ identifier.id }}</code><span v-if="!identifier.id">(empty)</span>
          </p>
          <p>Statement</p>
          <v-alert
            border="left"
            color="code">
            <pre>{{ statement }}</pre>
          </v-alert>
          <p>
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
          <strong>Result</strong>
        </p>
        <p>
          Hash: <code v-if="result_hash">{{ result_hash }}</code><span v-if="!result_hash">(empty)</span>
        </p>
        <p>
          Rows: <code v-if="result_number">{{ result_number }}</code><span v-if="!result_number">(empty)</span>
        </p>
        <p>
          Executed: <code v-if="execution">{{ execution }}</code><span v-if="!execution">(empty)</span>
        </p>
        <p v-if="creator">
          Owner: <code>{{ creator.username }}</code><span v-if="!creator.username">(empty)</span>
        </p>
        <p class="mt-2">
          <strong>Creator(s)</strong>
        </p>
        <p v-if="identifier.creators.length === 0">
          (empty) &#8212; <a href="#" @click.stop="openDialog()">modify</a>
        </p>
        <p v-for="(creator, i) in creators" :key="i">
          <OrcidIcon :orcid="creator.orcid" />
          <span>{{ creator.lastname }} {{ creator.firstname }}</span>
          <sup v-if="creator.affiliation">{{ creator.affiliation }}</sup>
        </p>
      </v-card-text>
      <QueryResults v-if="identifier.visibility !== 'SELF'" ref="queryResults" v-model="query.id" class="ml-2 mr-2 mt-0" />
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
        creator: null
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
        doi: null,
        creators: []
      },
      database: {
        id: null,
        name: null,
        is_public: null
      },
      persistQueryExists: false,
      persistQueryDialog: false,
      loading: true,
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
    this.loadMetadata()
  },
  methods: {
    formatDate (d) {
      return format(new Date(d), 'dd.MM.yyyy HH:mm:ss')
    },
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        console.debug('database', res.data)
        this.database = res.data
      } catch (err) {
        this.error = true
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
    },
    async loadQuery () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, this.config)
        console.debug('query', res.data)
        this.query = res.data
      } catch (err) {
        this.error = true
        console.error('Could not load query', err)
        this.$toast.error('Could not load query')
      }
      this.loading = false
    },
    async loadMetadata () {
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

      // refresh QueryResults table
      setTimeout(() => {
        this.$refs.queryResults.execute()
      }, 200)
    },
    async reExecute () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, {}, this.config)
        console.debug('re-execute query', res.data)
      } catch (err) {
        console.error('Could not re-execute query', err)
        this.$toast.error('Could not re-execute query')
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
</style>
