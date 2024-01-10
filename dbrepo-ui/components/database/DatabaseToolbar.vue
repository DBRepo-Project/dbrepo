<template>
  <div v-if="database">
    <v-toolbar flat>
      <v-toolbar-title>
        <span v-if="$vuetify.breakpoint.lgAndUp">{{ database.name }}</span>
        <v-tooltip bottom>
          <template v-slot:activator="{ on, attrs }">
            <v-icon
              v-if="!database.is_public"
              class="mb-1"
              right
              v-bind="attrs"
              v-on="on">
              mdi-lock-outline
            </v-icon>
            <v-icon
              v-if="database.is_public"
              class="mb-1"
              color="success"
              right
              v-bind="attrs"
              v-on="on">
              mdi-lock-open-outline
            </v-icon>
          </template>
          <span>{{ $t('databases.tooltip.' + (database.is_public ? 'public' : 'private'), { name: 'vue-i18n' }) }}</span>
        </v-tooltip>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canImportCsv" class="mb-1" :to="`/database/${$route.params.database_id}/table/import`">
          <v-icon v-if="$vuetify.breakpoint.lgAndUp" left>mdi-cloud-upload</v-icon> Import .csv
        </v-btn>
        <DownloadButton
          v-if="database && database.identifier"
          :pid="database.identifier.id"
          class="mr-2 mb-1">
          <v-icon left>mdi-code-tags</v-icon> Identifier .xml
        </DownloadButton>
        <v-btn v-if="canCreateSubset" color="secondary" class="mb-1 white--text" :to="`/database/${$route.params.database_id}/query/create`">
          <v-icon v-if="$vuetify.breakpoint.lgAndUp" left>mdi-wrench</v-icon> Create Subset
        </v-btn>
        <v-btn v-if="canCreateView" color="secondary" class="mb-1 white--text" :to="`/database/${$route.params.database_id}/view/create`">
          <v-icon v-if="$vuetify.breakpoint.lgAndUp" left>mdi-view-carousel-outline</v-icon> Create View
        </v-btn>
        <v-btn v-if="canCreateTable" color="secondary" class="mb-1" :to="`/database/${$route.params.database_id}/table/create`">
          <v-icon v-if="$vuetify.breakpoint.lgAndUp" left>mdi-table-large-plus</v-icon> Create Table
        </v-btn>
        <v-btn v-if="canCreateIdentifier" color="primary" class="mb-1" :to="`/database/${$route.params.database_id}/persist`">
          <v-icon v-if="$vuetify.breakpoint.lgAndUp" left>mdi-identifier</v-icon> Get PID
        </v-btn>
      </v-toolbar-title>
      <template v-slot:extension>
        <v-tabs v-model="tab" color="primary">
          <v-tab :to="`/database/${$route.params.database_id}/info`">
            {{ $t('databases.toolbar.info', { name: 'vue-i18n' }) }}
          </v-tab>
          <v-tab :to="`/database/${$route.params.database_id}/table`">
            {{ $t('databases.toolbar.tables', { name: 'vue-i18n' }) }}
          </v-tab>
          <v-tab :to="`/database/${$route.params.database_id}/query`">
            {{ $t('databases.toolbar.subsets', { name: 'vue-i18n' }) }}
          </v-tab>
          <v-tab :to="`/database/${$route.params.database_id}/view`">
            {{ $t('databases.toolbar.views', { name: 'vue-i18n' }) }}
          </v-tab>
          <v-tab v-if="isOwner" :to="`/database/${$route.params.database_id}/settings`">
            {{ $t('databases.toolbar.settings', { name: 'vue-i18n' }) }}
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
import DownloadButton from '@/components/identifier/DownloadButton.vue'

export default {
  components: {
    DownloadButton
  },
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
    roles () {
      return this.$store.state.roles
    },
    canCreateIdentifier () {
      if (!this.roles) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      return this.roles.includes('create-identifier') && this.isOwner
    },
    canDeleteIdentifier () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('delete-identifier')
    },
    hasIdentifier () {
      return this.database && 'identifiers' in this.database && this.database.identifiers.length > 0
    },
    hasWriteAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canImportCsv () {
      if (!this.user || !this.hasWriteAccess) {
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
      if (!this.user || !this.isOwner) {
        return false
      }
      return this.roles.includes('create-database-view')
    },
    canCreateTable () {
      if (!this.user || !this.hasWriteAccess) {
        return false
      }
      return this.roles.includes('create-table')
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      return this.database.owner.username === this.user.username
    }
  }
}
</script>
