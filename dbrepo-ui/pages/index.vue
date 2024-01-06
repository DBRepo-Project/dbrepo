<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title v-text="$t('databases.recent', { name: 'vue-i18n' })" />
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canCreateDatabase" color="primary" name="create-database" @click.stop="createDbDialog = true">
          <v-icon left>mdi-plus</v-icon> Database
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat tile>
      <v-divider class="mx-4" />
      <v-card-text v-if="infoLinks && infoLinks.length > 0">
        <div class="mb-2">Important Links</div>
        <div class="text--primary">
          <ul>
            <li v-for="(link, i) in infoLinks" :key="i">
              <a :href="link.href" :target="link.blank ? '_blank' : 'self'">
                {{ link.text }} <sup v-if="link.blank"><v-icon color="primary" x-small>mdi-open-in-new</v-icon></sup>
              </a>
            </li>
          </ul>
        </div>
      </v-card-text>
    </v-card>
    <DatabaseList ref="databases" :databases="databases" />
    <v-dialog
      v-model="createDbDialog"
      persistent
      max-width="640">
      <CreateDB @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import DatabaseList from '@/components/DatabaseList'
import CreateDB from '@/components/dialogs/CreateDB'
import DatabaseService from '@/api/database.service'

export default {
  components: {
    CreateDB,
    DatabaseList
  },
  data () {
    return {
      loading: false,
      createDbDialog: null,
      databases: []
    }
  },
  computed: {
    roles () {
      return this.$store.state.roles
    },
    canCreateDatabase () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database')
    },
    infoLinks () {
      const infoLinks = this.$config.infoLinks
      console.debug('info links', infoLinks)
      return infoLinks
    },
    isFiltered () {
      return this.$route.query.f === 'my'
    }
  },
  mounted () {
    this.loadDatabases()
  },
  methods: {
    closed (event) {
      this.createDbDialog = false
      if (event.success) {
        this.$router.push('/database?f=my')
      }
    },
    loadDatabases () {
      this.loadingDatabases = true
      if (this.isFiltered) {
        DatabaseService.findAllOnlyAccess()
          .then((databases) => {
            this.databases = databases
            console.info('Found', this.databases.length, 'database(s) with access')
          })
          .finally(() => {
            this.loadingDatabases = false
          })
      } else {
        DatabaseService.findAll()
          .then((databases) => {
            this.databases = databases
            console.info('Found', this.databases.length, 'database(s)')
          })
          .finally(() => {
            this.loadingDatabases = false
          })
      }
    }
  }
}
</script>
