<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>{{ identifier.title }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="!identifier.id && !loading" color="blue-grey white--text" class="mr-2" :disabled="!query.execution || !token" @click.stop="openDialog()">
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
        <span v-if="query.execution == null">
          Query was never executed
        </span>
      </v-card-subtitle>
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
          <p v-if="identifier.id">
            Persistent Identifier: <code>https://dbrepo.ossdip.at/pid/{{ identifier.id }}</code>
          </p>
          <p v-if="identifier.publication_year">
            Publication Year: <code>{{ identifier.publication_year }}</code>
          </p>
          <p>Statement</p>
          <v-alert
            border="left"
            color="grey lighten-4 black--text">
            <pre>{{ query.query }}</pre>
          </v-alert>
          <p v-if="query.query_hash">
            Hash: <code>sha256:{{ query.query_hash }}</code>
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
        <p v-if="query.result_hash">
          Hash: <code v-if="query.result_hash">sha256:{{ query.result_hash }}</code>
        </p>
        <p>
          Rows: <code v-if="query.result_number">{{ query.result_number }}</code><span v-if="!query.result_number">(empty)</span>
        </p>
        <p v-if="execution">
          Executed: <code>{{ execution }}</code>
        </p>
        <p>
          Owner: <code v-if="query.creator.username">{{ query.creator.username }}</code><span v-if="!query.creator.username">(empty)</span>
        </p>
        <p class="mt-2">
          <strong>Creator(s)</strong>
        </p>
        <p v-if="identifier.creators.length === 0">
          (empty) &#8212; <a href="#" @click.stop="openDialog()">modify</a>
        </p>
        <p v-for="(creator,i) in identifier.creators" :key="i">
          <span>{{ creator.name }}</span>
          <sup v-if="creator.affiliation">{{ creator.affiliation }}</sup>
        </p>
      </v-card-text>
      <QueryResults ref="queryResults" v-model="query.id" class="ml-2 mr-2 mt-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="persistQueryDialog"
      persistent
      max-width="640">
      <PersistQuery @close="closeDialog" />
    </v-dialog>
  </div>
</template>
<script>
import { format } from 'date-fns'
import PersistQuery from '@/components/dialogs/PersistQuery'

export default {
  name: 'QueryShow',
  components: {
    PersistQuery
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
        created: null
      },
      identifier: {
        id: null,
        dbid: null,
        qid: null,
        title: null,
        description: null,
        visibility: null,
        publication_year: null,
        doi: null,
        creators: []
      },
      database: {
        id: null,
        publisher: null
      },
      persistQueryExists: false,
      persistQueryDialog: false,
      loading: true
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    headers () {
      if (this.token === null) {
        return null
      }
      return { Authorization: `Bearer ${this.token}` }
    },
    execution () {
      if (this.query.execution === null) {
        return null
      }
      return this.formatDate(this.query.execution)
    }
  },
  mounted () {
    this.loadMetadata()
  },
  methods: {
    formatDate (d) {
      return format(new Date(d), 'dd.MM.yyyy HH:mm:ss')
    },
    async loadMetadata () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`)
        console.debug('query', res.data)
        this.query = res.data
      } catch (err) {
        console.error('Could not load query', err)
        this.$toast.error('Could not load query')
        this.loading = false
      }
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`)
        console.debug('database', res.data)
        this.database = res.data
      } catch (err) {
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
        this.loading = false
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/identifier?qid=${this.$route.params.query_id}`)
        this.identifier = res.data[0]
        console.debug('identifier', res.data[0])
      } catch (err) {
        if (err.response.status !== 404) {
          console.error('Could not load identifier', err)
          this.$toast.error('Could not load identifier')
        }
        this.loading = false
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
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, {}, {
          headers: this.headers
        })
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
pre {
  white-space: break-spaces;
}
</style>
