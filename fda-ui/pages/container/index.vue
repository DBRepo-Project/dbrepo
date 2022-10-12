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
    <v-toolbar dense flat>
      <v-toolbar-items>
        <span class="mr-4">Filter:</span>
        <v-checkbox
          v-model="filterPrivate"
          label="Private" />
        <v-checkbox
          v-if="user.username"
          v-model="filterMine"
          class="ml-2"
          label="Mine" />
      </v-toolbar-items>
    </v-toolbar>
    <v-card flat>
      <v-data-table
        :headers="headers"
        :items="filter(databases)"
        @click:row="loadDatabase">
        <template v-slot:item.visibility="{ item }">
          <v-icon v-if="!item.visibility" color="primary" title="Private" class="private-icon" right>mdi-lock-outline</v-icon>
          <v-icon v-if="item.visibility" class="private-icon" title="Public" right>mdi-lock-open-outline</v-icon>
        </template>
        <template v-slot:item.creator="{ item }">
          <span>{{ formatCreator(item.creator) }}</span>
          <sup>
            <v-icon v-if="item.creator.email_verified" small color="primary">mdi-check-decagram</v-icon>
          </sup>
        </template>
      </v-data-table>
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
import { decodeJwt } from 'jose'

export default {
  components: {
    CreateDB
  },
  data () {
    return {
      loadingContainers: false,
      createDbDialog: false,
      databases: [],
      containers: [],
      filterPrivate: false,
      filterMine: false,
      searchQuery: null,
      user: {
        username: null
      },
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
    },
    headers () {
      return [{
        text: 'Name',
        align: 'start',
        value: 'name'
      }, {
        text: 'Creator',
        value: 'creator',
        sortable: false
      }, {
        text: 'Visibility',
        value: 'visibility'
      }, {
        text: 'Created',
        value: 'created'
      }]
    }
  },
  mounted () {
    this.loadUser()
    this.loadContainers()
      .then(() => this.loadDatabases())
  },
  methods: {
    formatCreator (creator) {
      return formatUser(creator)
    },
    filter (databases) {
      let filtered = databases
      if (this.filterPrivate) {
        filtered = filtered.filter(d => d.visibility === false)
      }
      if (this.token && this.filterMine) {
        filtered = filtered.filter(d => d.creator.username === this.user.username)
      }
      return filtered
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
      this.loading = true
      for (const container of this.containers) {
        try {
          const res = await this.$axios.get(`/api/container/${container.id}/database`, this.config)
          for (const info of res.data) {
            info.container_id = container.id
            info.visibility = info.is_public
            info.created = formatTimestampUTCLabel(info.created)
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
      this.$router.push(`/container/${database.container_id}/database/${database.id}`)
    },
    loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
    }
  }
}
</script>

<style>
  tbody tr {
    cursor: pointer;
  }
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
