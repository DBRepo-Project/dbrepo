<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <span>Databases</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="isResearcher" color="primary" @click.stop="createDbDialog = true">
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
          v-if="user"
          v-model="filterMine"
          class="ml-2"
          label="Mine" />
      </v-toolbar-items>
    </v-toolbar>
    <v-progress-linear v-if="loadingContainers || loadingDatabases" :color="loadingColor" :indeterminate="!error" />
    <v-card
      v-for="(container, idx) in filter(containers)"
      :key="idx"
      :to="`/container/${container.id}/database/${container.database.id}`"
      :disabled="!container.database"
      flat
      tile>
      <v-divider class="mx-4" />
      <v-card-title v-if="notInit(container)" v-text="container.name" />
      <v-card-title v-if="!notInit(container)">
        <a :href="`/container/${container.id}/database/${container.database.id}`">{{ container.name }}</a>
      </v-card-title>
      <v-card-subtitle v-if="!container.database.identifier" class="db-subtitle" v-text="formatCreator(container.creator)" />
      <v-card-subtitle v-if="container.database.identifier" class="db-subtitle" v-text="formatCreatorz(container)" />
      <v-card-text class="db-description">
        <div class="db-tags">
          <v-chip v-if="!notInit(container) && container.database.is_public" small color="green" outlined>Public</v-chip>
          <v-chip v-if="!notInit(container) && !container.database.is_public" small color="red" outlined>Private</v-chip>
          <v-chip v-if="identifierCreated(container)" small outlined>Database</v-chip>
          <v-chip v-if="identifierCreated(container)" small outlined v-text="identifierCreated(container)" />
        </div>
        <div v-text="identifierDescription(container)" />
      </v-card-text>
      <v-card-text v-if="canInit(container)" class="db-buttons">
        <v-btn
          small
          secondary
          :loading="container.database.loading"
          @click.stop="initDatabase(container)">
          Start
        </v-btn>
      </v-card-text>
    </v-card>
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
import { formatTimestampUTCLabel, formatCreators, formatYearUTC, formatUser, isResearcher } from '@/utils'

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
    user () {
      return this.$store.state.user
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
    formatCreatorz (container) {
      return formatCreators(container)
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
    async initDatabase (container) {
      await this.startContainer(container)
        .then(() => this.createDatabase(container))
    },
    filter (containers) {
      if (this.loadingDatabases) {
        return []
      }
      let filtered = containers
      if (this.filterPrivate) {
        filtered = filtered.filter(c => !c.database.is_public)
      }
      if (this.user && this.filterMine) {
        filtered = filtered.filter(c => c.creator.username === this.user.username)
      }
      return filtered
    },
    identifierCreated (container) {
      if (!container.database.identifier) {
        return null
      }
      return formatYearUTC(container.database.identifier.created)
    },
    identifierDescription (container) {
      if (!container.database.identifier) {
        return null
      }
      return container.database.identifier.description
    },
    async loadContainers () {
      this.createDbDialog = false
      try {
        this.loadingContainers = true
        const res = await this.$axios.get('/api/container/')
        this.containers = res.data.map((container) => {
          container.database = {
            id: null,
            loading: false
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
    async startContainer (container) {
      try {
        container.database.loading = true
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
      container.database.loading = false
    },
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
        this.loadContainers()
          .then(() => this.loadDatabases())
      }
    }
  }
}
</script>
<style scoped>
  tbody tr {
    cursor: pointer;
  }
  .v-chip:not(:first-child) {
    margin-left: 8px;
  }
  .db-subtitle {
    padding-bottom: 8px;
  }
  .db-tags {
    margin-bottom: 8px;
  }
</style>
