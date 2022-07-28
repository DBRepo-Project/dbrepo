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
              <th>Visibility</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="databases.length === 0" aria-readonly="true">
              <td colspan="5">
                <span>(no databases)</span>
              </td>
            </tr>
            <tr
              v-for="item in databases"
              :key="item.id"
              class="database"
              @click="loadDatabase(item)">
              <td>
                <span>{{ item.name }}</span>
              </td>
              <td>
                <span>{{ item.engine }}</span>
              </td>
              <td>
                <span>{{ formatCreator(item.creator) }}</span>
                <sup>
                  <v-icon v-if="item.creator.email_verified" small color="primary">mdi-check-decagram</v-icon>
                </sup>
              </td>
              <td>
                <v-icon v-if="!item.is_public" color="primary" title="Private" class="private-icon" right>mdi-lock-outline</v-icon>
                <v-icon v-if="item.is_public" class="private-icon" title="Public" right>mdi-lock-open-outline</v-icon>
              </td>
              <td>
                {{ createdUTC(item.created) }}
              </td>
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
import { formatTimestampUTCLabel, formatUser } from '@/utils'

export default {
  components: {
    CreateDB
  },
  data () {
    return {
      createDbDialog: false,
      databases: [],
      containers: [],
      searchQuery: null,
      items: [
        { text: 'Databases', to: '/container', activeClass: '' }
      ],
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
      return formatUser(creator)
    },
    search () {
      console.debug('search for', this.searchQuery)
    },
    async loadContainers () {
      this.createDbDialog = false
      try {
        this.loading = true
        const res = await this.$axios.get('/api/container/')
        this.containers = res.data
        console.debug('containers', this.containers)
        this.error = false
      } catch (err) {
        console.error('containers', err)
        this.error = true
        this.loading = false
      }
    },
    async loadDatabases () {
      if (this.containers.length === 0) {
        return
      }
      this.loading = true
      for (const container of this.containers) {
        try {
          const res = await this.$axios.get(`/api/container/${container.id}/database`, this.config)
          for (const info of res.data) {
            info.container_id = container.id
            this.databases.push(info)
          }
        } catch (err) {
          if (err.response === undefined || err.response.status === undefined || err.response.status !== 401) {
            console.error('Failed to load databases for container', err)
          }
        }
      }
      this.loading = false
      console.debug('databases', this.databases)
    },
    createdUTC (str) {
      return formatTimestampUTCLabel(str)
    },
    loadDatabase (database) {
      this.$router.push(`/container/${database.container_id}/database/${database.id}/info`)
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
  .private-icon {
    flex: 0 !important;
    margin-right: 16px;
  }
</style>
