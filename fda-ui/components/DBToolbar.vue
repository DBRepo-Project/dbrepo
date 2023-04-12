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
          Import .csv
        </v-btn>
        <v-btn v-if="canCreateSubset" color="secondary" class="mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create`">
          Create Subset
        </v-btn>
        <v-btn v-if="canCreateView" color="secondary" class="ml-2 mr-2 mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/create`">
          Create View
        </v-btn>
        <v-btn v-if="canCreateTable" color="primary" class="mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/create`">
          Create Table
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
    roles () {
      return this.$store.state.roles
    },
    canImportCsv () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    canCreateSubset () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('execute-query')
    },
    canCreateView () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('create-database-view')
    },
    canCreateTable () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('create-table')
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      return this.database.owner.username === this.user.username
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
