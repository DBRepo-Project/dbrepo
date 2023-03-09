<template>
  <div>
    <v-progress-linear v-if="loadingContainers || loadingDatabases" :color="loadingColor" :indeterminate="!error" />
    <v-card
      v-for="(container, idx) in containers"
      :key="idx"
      :to="link(container)"
      :disabled="!container.database"
      flat
      tile>
      <v-divider class="mx-4" />
      <v-card-title v-if="!hasDatabase(container)" v-text="container.name" />
      <v-card-title v-if="hasDatabase(container)">
        <a :href="`/container/${container.id}/database/${container.database.id}`">{{ container.name }}</a>
      </v-card-title>
      <v-card-subtitle v-if="!hasIdentifier(container)" class="db-subtitle" v-text="formatCreator(container.creator)" />
      <v-card-subtitle v-if="hasIdentifier(container)" class="db-subtitle" v-text="formatCreatorz(container)" />
      <v-card-text class="db-description">
        <div class="db-tags">
          <v-chip v-if="hasDatabase(container) && container.database.is_public" small color="green" outlined>Public</v-chip>
          <v-chip v-if="hasDatabase(container) && !container.database.is_public" small color="red" outlined>Private</v-chip>
          <v-chip v-if="hasIdentifier(container)" small outlined>PID</v-chip>
          <v-chip
            v-if="identifierCreated(container)"
            small
            outlined
            v-text="container.database.identifier.publisher" />
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
      <v-divider v-if="idx - 1 === databases.length" class="mx-4" />
    </v-card>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn
          small
          color="secondary">
          More
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
  </div>
</template>

<script>
import { formatCreators, formatUser, formatYearUTC, isResearcher } from '@/utils'

export default {
  data () {
    return {
      loadingContainers: false,
      loadingCreate: false,
      createDbDialog: false,
      databases: [],
      containers: [],
      searchQuery: null,
      createDatabaseDto: {
        name: null,
        is_public: true
      },
      items: [
        { text: 'Databases', to: '/container', activeClass: '' }
      ],
      loadingDatabases: false,
      error: false
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
  },
  methods: {
    formatCreator (creator) {
      return formatUser(creator)
    },
    formatCreatorz (container) {
      const creators = formatCreators(container)
      return creators || this.formatCreator(container.creator)
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
    hasDatabase (container) {
      return container.database
    },
    hasIdentifier (container) {
      return container.database && container.database.identifier
    },
    async initDatabase (container) {
      await this.startContainer(container)
        .then(() => this.createDatabase(container))
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
        const res = await this.$axios.get('/api/container?limit=100')
        this.containers = res.data
        console.debug('containers', this.containers)
        this.error = false
      } catch (error) {
        this.error = true
        console.error('Failed to retrieve containers', error)
        const { message } = error.response.data
        this.$toast.error(`Failed to retrieve containers: ${message}`)
      }
      this.loadingContainers = false
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
    link (container) {
      if (!container.database || !container.database.id) {
        return null
      }
      return `/container/${container.id}/database/${container.database.id}`
    }
  }
}
</script>

<style>
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
