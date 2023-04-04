<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        Recent Databases
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="isResearcher" color="primary" name="create-database" @click.stop="createDbDialog = true">
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
import { isResearcher } from '@/utils'

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
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    isResearcher () {
      return isResearcher(this.user)
    }
  },
  methods: {
    async createDatabase (container) {
      try {
        container.database.loading = true
        this.createDatabaseDto.name = container.name
        const res = await this.$axios.post(`/api/container/${container.id}/database`, this.createDatabaseDto, this.config)
        container.database = res.data
        console.debug('created database', container.database)
        this.error = false
      } catch (error) {
        const { message } = error.response
        this.error = true
        console.error('Failed to create database', error)
        this.$toast.error(`${message}`)
      }
      container.database.loading = false
    },
    closed (event) {
      this.createDbDialog = false
      if (event.success) {
        this.$refs.databases.loadContainers()
      }
    }
  }
}
</script>
