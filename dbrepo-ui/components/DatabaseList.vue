<template>
  <div>
    <v-progress-linear v-if="loadingDatabases" :indeterminate="!error" />
    <v-card v-if="!$vuetify.theme.dark && containers.length> 0" flat tile>
      <v-divider class="mx-4" />
    </v-card>
    <v-card
      v-for="(database, idx) in databases"
      :key="idx"
      :to="link(database)"
      flat
      tile>
      <v-divider v-if="idx !== 0" class="mx-4" />
      <v-card-title>
        <a :href="`/database/${database.id}`">{{ database.name }}</a>
      </v-card-title>
      <v-card-subtitle class="db-subtitle" v-text="formatCreators(database)" />
      <v-card-text class="db-description">
        <div class="db-tags">
          <v-chip v-if="database.is_public" small color="green" outlined>Public</v-chip>
          <v-chip v-if="!database.is_public" small color="red" outlined>Private</v-chip>
          <v-chip
            v-if="identifierYear(database)"
            small
            outlined
            v-text="identifierYear(database)" />
          <v-chip
            v-if="hasIdentifier(database)"
            small
            outlined
            v-text="database.identifier.publisher" />
        </div>
        <div v-text="identifierDescription(database)" />
      </v-card-text>
    </v-card>
    <v-toolbar v-if="false" flat>
      <v-toolbar-title>
        <v-btn
          small
          color="secondary">
          More
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
  </div>
</template>

<script>
import DatabaseService from '@/api/database.service'
import DatabaseMapper from '@/api/database.mapper'

export default {
  data () {
    return {
      loadingDatabases: false,
      loadingCreate: false,
      createDbDialog: false,
      databases: [],
      searchQuery: null,
      limit: 100,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' }
      ],
      error: false
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    }
  },
  mounted () {
    this.loadDatabases()
  },
  methods: {
    formatCreators (database) {
      return DatabaseMapper.databaseToOwner(database)
    },
    hasIdentifier (database) {
      return database && database.identifier
    },
    identifierYear (database) {
      if (!database || !database.identifier || !database.identifier.publication_year) {
        return null
      }
      return database.identifier.publication_year
    },
    identifierDescription (database) {
      if (!database || !database.identifier) {
        return null
      }
      return database.identifier.description
    },
    loadDatabases () {
      this.createDbDialog = false
      this.loadingDatabases = true
      DatabaseService.findAll() // TODO: write a findAllDatabases method
        .then((databases) => {
          this.databases = databases
          console.info('Found', this.databases.length, 'container(s)')
        })
      this.loadingDatabases = false
    },
    link (database) {
      if (!database || !database.container) {
        return null
      }
      return `/database/${database.id}`
    }
  }
}
</script>

<style>
.v-chip:not(:first-child) {
  margin-left: 8px;
}
.db-subtitle {
  padding-bottom: 8px;
}
.db-tags {
  margin-bottom: 8px;
}
</style>
