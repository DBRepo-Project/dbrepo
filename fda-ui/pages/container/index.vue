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
        :items="filter(containers)"
        :loading="loadingDatabases"
        @click:row="loadDatabase">
        <template v-slot:item.visibility="{ item }">
          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-icon
                v-if="item.visibility"
                color="primary"
                class="private-icon"
                right
                v-bind="attrs"
                v-on="on">
                mdi-lock-outline
              </v-icon>
              <v-icon
                v-if="!item.visibility"
                class="private-icon"
                right
                v-bind="attrs"
                v-on="on">
                mdi-lock-open-outline
              </v-icon>
            </template>
            <span>{{ tooltip(item) }}</span>
          </v-tooltip>
        </template>
        <template v-slot:item.created="{ item }">
          <span>{{ formatTimestamp(item.created) }}</span>
        </template>
        <template v-slot:item.creator="{ item }">
          <span>{{ formatCreator(item.creator) }}</span>
        </template>
        <template v-slot:item.status="{ item }">
          <span
            v-if="notInit(item) && !canInit(item)">
            Not Initialized
          </span>
          <v-btn
            v-if="canInit(item)"
            color="secondary"
            :loading="loadingCreate"
            small
            @click.stop="initDatabase(item)">
            Start
          </v-btn>
        </template>
      </v-data-table>
      <v-dialog
        v-model="createDbDialog"
        persistent
        max-width="640">
        <CreateDB @close="closed" />
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
      loadingCreate: false,
      createDbDialog: false,
      databases: [],
      containers: [],
      filterPrivate: false,
      filterMine: false,
      searchQuery: null,
      createDatabaseDto: {
        name: null,
        is_public: true
      },
      user: {
        username: null
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
      }, {
        text: 'Status',
        value: 'status',
        sortable: false
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
    canInit (container) {
      if (!this.token) {
        return false
      }
      if (container.creator.username !== this.user.username) {
        return false
      }
      return !container.database.id && !this.loadingDatabases
    },
    notInit (container) {
      return !container.database.id
    },
    tooltip (item) {
      return item.is_public ? 'Public' : 'Private'
    },
    async initDatabase (container) {
      await this.startContainer(container)
        .then(() => this.createDatabase(container))
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
        this.containers = res.data.map((container) => {
          container.database = {
            id: null
          }
          return container
        })
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
      this.loadingDatabases = true
      for (const container of this.containers) {
        try {
          const res = await this.$axios.get(`/api/container/${container.id}/database`, this.config)
          for (const info of res.data) {
            info.container_id = container.id
            info.visibility = info.is_public
            info.created = formatTimestampUTCLabel(info.created)
            const filtered = this.containers.filter(c => c.id === container.id)[0]
            filtered.database = info
          }
        } catch (err) {
          if (err.response === undefined || err.response.status === undefined || err.response.status !== 401) {
            console.error('Failed to load databases for container', err)
          }
        }
      }
      this.loadingDatabases = false
      console.debug('containers with databases', this.containers)
    },
    formatTimestamp (str) {
      return formatTimestampUTCLabel(str)
    },
    loadDatabase (container) {
      if (this.notInit(container)) {
        console.warn('Container', container.id, 'not initialized')
        return
      }
      this.$router.push(`/container/${container.id}/database/${container.database.id}`)
    },
    loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
    },
    async startContainer (container) {
      try {
        this.loadingCreate = true
        const res = await this.$axios.put(`/api/container/${container.id}`, { action: 'start' }, this.config)
        console.debug('started container', res.data)
        this.error = false
      } catch (error) {
        const { status } = error.response
        if (status !== 409) {
          this.error = true
          this.$toast.error('Failed to start container')
        }
      }
      this.loadingCreate = false
    },
    async createDatabase (container) {
      try {
        this.loadingCreate = true
        this.createDatabaseDto.name = container.name
        const res = await this.$axios.post(`/api/container/${container.id}/database`, this.createDatabaseDto, this.config)
        container.database = res.data
        console.debug('created database', container.database)
        this.error = false
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to create database')
      }
      this.loadingCreate = false
    },
    closed (event) {
      this.createDbDialog = false
      if (event.success) {
        this.loadContainers()
          .then(() => this.loadDatabases())
      }
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
