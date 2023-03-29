<template>
  <div v-if="database">
    <v-toolbar flat>
      <v-toolbar-title>
        <span>{{ database.name }}</span>
        <v-tooltip bottom>
          <template v-slot:activator="{ on, attrs }">
            <v-icon
              v-if="!database.is_public"
              color="primary"
              class="mb-1"
              right
              v-bind="attrs"
              v-on="on">
              mdi-lock-outline
            </v-icon>
            <v-icon
              v-if="database.is_public"
              class="mb-1"
              right
              v-bind="attrs"
              v-on="on">
              mdi-lock-open-outline
            </v-icon>
          </template>
          <span>{{ databaseTooltip }}</span>
        </v-tooltip>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canImportCsv" class="mr-2 mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/import`">
          <v-icon left>mdi-cloud-upload</v-icon> Import CSV
        </v-btn>
        <v-btn v-if="canCreateSubset" color="secondary" class="mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create`">
          <v-icon left>mdi-wrench</v-icon> Create Subset
        </v-btn>
        <v-btn v-if="canCreateView" color="secondary" class="ml-2 mr-2 mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/create`">
          <v-icon left>mdi-view-carousel-outline</v-icon> Create View
        </v-btn>
        <v-btn v-if="canCreateTable" color="primary" class="mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/create`">
          <v-icon left>mdi-table-large-plus</v-icon> Create Table
        </v-btn>
      </v-toolbar-title>
      <template v-slot:extension>
        <v-tabs v-model="tab" color="primary">
          <v-tab :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/info`">
            Info
          </v-tab>
          <v-tab :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table`">
            Tables
          </v-tab>
          <v-tab :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query`">
            Subsets
          </v-tab>
          <v-tab :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view`">
            Views
          </v-tab>
          <v-tab v-if="isOwner" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/settings`">
            Settings
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
export default {
  data () {
    return {
      tab: null,
      error: false
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    access () {
      return this.$store.state.access
    },
    user () {
      return this.$store.state.user
    },
    token () {
      return this.$store.state.token
    },
    canImportCsv () {
      return this.user.roles.includes('insert-table-data')
    },
    canCreateSubset () {
      return this.user.roles.includes('execute-query')
    },
    canCreateView () {
      return this.user.roles.includes('create-database-view')
    },
    canCreateTable () {
      return this.user.roles.includes('create-table')
    },
    isOwner () {
      if (!this.user || !this.database || !this.database.creator) {
        return false
      }
      return this.database.creator.username === this.user.client_id
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    silentConfig () {
      return {
        headers: this.config.headers,
        progress: false
      }
    },
    databaseTooltip () {
      return this.database.is_public ? 'Public' : 'Private'
    }
  }
}
</script>
