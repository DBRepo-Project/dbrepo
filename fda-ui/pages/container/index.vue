<template>
  <div>
    <v-progress-linear v-if="loadingContainers" :color="loadingColor" :indeterminate="!error" />
    <v-toolbar flat>
      <v-toolbar-title>
        <span>Databases</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="token" color="primary" @click.stop="createDbDialog = true">
          <v-icon left>mdi-plus</v-icon> Database
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat>
      <v-simple-table>
        <template v-slot:default>
          <thead>
            <tr>
              <th>Name</th>
              <th>Engine</th>
              <th>Creator</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="containers.length === 0" aria-readonly="true">
              <td colspan="4">
                <span v-if="!loading">(no databases)</span>
              </td>
            </tr>
            <tr
              v-for="item in containers"
              :key="item.id"
              class="database"
              @click="loadDatabase(item)">
              <td>{{ item.name }}</td>
              <td>
                <span v-if="item.database">{{ item.database.engine }}</span>
                <v-skeleton-loader v-if="!item.database" type="text" width="100" class="mt-1" />
              </td>
              <td>
                <span v-if="item.database">{{ formatCreator(item.database.creator) }}</span>
                <v-skeleton-loader v-if="!item.database" type="text" width="100" class="mt-1" />
                <sup v-if="item.database">
                  <v-icon v-if="item.database.creator.email_verified" small color="primary">mdi-check-decagram</v-icon>
                </sup>
              </td>
              <td>{{ createdUTC(item.created) }}</td>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
      <v-dialog
        v-model="createDbDialog"
        persistent
        max-width="640">
        <CreateDB @close="createDbDialog = false" />
      </v-dialog>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import { mdiDatabaseArrowRightOutline } from '@mdi/js'
import CreateDB from '@/components/dialogs/CreateDB'
import { formatTimestampUTC } from '@/utils'

export default {
  components: {
    CreateDB
  },
  data () {
    return {
      createDbDialog: false,
      containers: [],
      items: [
        { text: 'Databases', to: '/container', activeClass: '' }
      ],
      loadingContainers: false,
      loading: false,
      error: false,
      iconSelect: mdiDatabaseArrowRightOutline
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    }
  },
  mounted () {
    this.loadContainers()
      .then(() => this.loadDatabases())
  },
  methods: {
    formatCreator (creator) {
      if (creator.firstname && creator.lastname) {
        let name = ''
        if (creator.titles_before) {
          name += creator.titles_before + ' '
        }
        name += creator.firstname + ' ' + creator.lastname
        if (creator.titles_after) {
          name += ' ' + creator.titles_after
        }
        return name
      }
      return creator.username
    },
    async loadContainers () {
      this.createDbDialog = false
      try {
        this.loadingContainers = true
        const res = await this.$axios.get('/api/container/')
        this.containers = res.data
        console.debug('containers', this.containers)
        this.error = false
      } catch (err) {
        console.error('containers', err)
        this.error = true
      }
      this.loadingContainers = false
    },
    async loadDatabases () {
      if (this.containers.length === 0) {
        return
      }
      const containers = []
      for (const container of this.containers) {
        try {
          const res = await this.$axios.get(`/api/container/${container.id}/database`, this.config)
          for (const database of res.data) {
            container.database = database
            containers.push(container)
          }
        } catch (err) {
          if (err.response === undefined || err.response.status === undefined || err.response.status !== 401) {
            console.error('Failed to load databases for container', err)
          }
        }
      }
      this.containers = containers
      console.debug('databases loaded', this.containers)
    },
    createdUTC (str) {
      return formatTimestampUTC(str)
    },
    loadDatabase (container) {
      this.$router.push(`/container/${container.id}/database/${container.database.id}/info`)
    }
  }
}
</script>

<style>
  .trim {
    max-width: 10em;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .database:hover {
    cursor: pointer;
  }
  .color-grey {
    color: #aaa;
  }
  .v-progress-circular {
    margin-left: 8px;
  }
</style>
