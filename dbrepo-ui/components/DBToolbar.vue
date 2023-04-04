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
        <v-btn v-if="!loading && canModify && isResearcher" class="mr-2 mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/import`">
          <v-icon left>mdi-cloud-upload</v-icon> Import CSV
        </v-btn>
        <DownloadButton
          v-if="database?.identifier"
          :pid="database.identifier.id"
          color="secondary"
          class="mr-2 mb-1 white--text">
          <v-icon left>mdi-code-tags</v-icon> Identifier .xml
        </DownloadButton>
        <v-btn v-if="!loading && canRead && isResearcher" color="secondary" class="mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create`">
          <v-icon left>mdi-wrench</v-icon> Create Subset
        </v-btn>
        <v-btn v-if="!loading && isOwner && isResearcher" color="secondary" class="ml-2 mr-2 mb-1 white--text" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/create`">
          <v-icon left>mdi-view-carousel-outline</v-icon> Create View
        </v-btn>
        <v-btn v-if="!loading && canModify && isResearcher" color="primary" class="mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/create`">
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
import { isResearcher } from '@/utils'
import DownloadButton from '@/components/identifier/DownloadButton.vue'

export default {
  components: { DownloadButton },
  data () {
    return {
      tab: null,
      loading: false,
      error: false
    }
  },
  computed: {
    loadingColor () {
      return 'primary'
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
    token () {
      return this.$store.state.token
    },
    canModify () {
      if (!this.user || !this.access || !this.database || !this.database.creator) {
        return false
      }
      if (this.database.creator.username === this.user.username) {
        return true
      }
      return this.access.type === 'write_own' || this.access.type === 'write_all'
    },
    canRead () {
      if (this.database?.is_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
    },
    isOwner () {
      if (!this.user || !this.database || !this.database.creator) {
        return false
      }
      return this.database.creator.username === this.user.username
    },
    isResearcher () {
      return isResearcher(this.user)
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
