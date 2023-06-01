<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        {{ $t('databases.recent', { name: 'vue-i18n' }) }}
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canCreateDatabase" color="primary" name="create-database" @click.stop="createDbDialog = true">
          <v-icon left>mdi-plus</v-icon> Database
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <DatabaseList ref="databases" />
    <v-dialog
      v-model="createDbDialog"
      persistent
      max-width="640">
      <CreateDB @close="closed" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import { mdiDatabaseArrowRightOutline } from '@mdi/js'
import CreateDB from '@/components/dialogs/CreateDB'
import DatabaseList from '@/components/DatabaseList'

export default {
  components: {
    CreateDB,
    DatabaseList
  },
  data () {
    return {
      loadingContainers: false,
      loadingCreate: false,
      createDbDialog: false,
      createDatabaseDto: {
        name: null,
        is_public: true
      },
      items: [
        { text: 'Databases', to: '/container', activeClass: '' }
      ],
      loadingDatabases: false,
      error: false,
      iconSelect: mdiDatabaseArrowRightOutline
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    canCreateDatabase () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-container') && this.roles.includes('create-database')
    }
  },
  methods: {
    closed (event) {
      this.createDbDialog = false
      if (event.success) {
        this.$refs.databases.loadContainers()
      }
    }
  }
}
</script>
