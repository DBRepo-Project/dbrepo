<template>
  <div>
    <v-progress-linear v-if="loadingContainers || loadingDatabases" :color="loadingColor" :indeterminate="!error" />
    <v-card
      v-for="(container, idx) in containers"
      :key="idx"
      :to="link(container)"
      flat
      tile>
      <v-divider class="mx-4" />
      <v-card-title v-if="!hasDatabase(container)" v-text="container.name" />
      <v-card-title v-if="hasDatabase(container)">
        <a :href="`/container/${container.id}/database/${container.database.id}`">{{ container.name }}</a>
      </v-card-title>
      <v-card-subtitle v-if="!hasIdentifier(container)" class="db-subtitle" v-text="formatOwner(container)" />
      <v-card-subtitle v-if="hasIdentifier(container)" class="db-subtitle" v-text="formatCreators(container)" />
      <v-card-text v-if="hasDatabase(container)" class="db-description">
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
          :loading="container?.loading"
          @click.stop="initDatabase(container)">
          Start
        </v-btn>
      </v-card-text>
    </v-card>
    <v-toolbar v-if="false" flat>
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
import { listContainers, startContainer } from '@/api/container'
import { createDatabase } from '@/api/database'

export default {
  data () {
    return {
      loadingContainers: false,
      loadingCreate: false,
      createDbDialog: false,
      databases: [],
      containers: [],
      searchQuery: null,
      limit: 100,
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
    formatOwner (container) {
      if (!('database' in container) || !container.database) {
        return formatUser(container.creator)
      }
      return formatUser(container.database?.owner)
    },
    formatCreators (container) {
      const creators = formatCreators(container)
      return creators || this.formatUser(container.creator)
    },
    canInit (container) {
      if (!this.user) {
        return false
      }
      if (container.creator.username !== this.user.username) {
        return false
      }
      return !container.database
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
      if (!container || !container.database || !container.database.identifier) {
        return null
      }
      return formatYearUTC(container.database.identifier.created)
    },
    identifierDescription (container) {
      if (!container || !container.database || !container.database.identifier) {
        return null
      }
      return container.database.identifier.description
    },
    async loadContainers () {
      this.createDbDialog = false
      try {
        this.loadingContainers = true
        const res = await listContainers(this.limit)
        this.containers = res.data
        console.debug('containers', this.containers)
        this.error = false
      } catch (error) {
        this.error = true
        console.error('Failed to retrieve containers', error)
        const { statusText } = error.response
        this.$toast.error(`Failed to retrieve containers: ${statusText}`)
      }
      this.loadingContainers = false
    },
    async startContainer (container) {
      try {
        container.loading = true
        const res = await startContainer(this.token, container.id)
        console.debug('started container', res.data)
        this.error = false
      } catch (error) {
        console.error('start container', error)
        const { status } = error.response
        if (status !== 409) {
          this.error = true
          this.$toast.error('Failed to start container')
        }
      }
      container.loading = false
    },
    async createDatabase (container) {
      try {
        container.loading = true
        const res = await createDatabase(this.token, container)
        container.database = res.data
        console.debug('created database', container.database)
        this.error = false
      } catch (error) {
        console.error('create database', error)
        const { message } = error.response
        this.error = true
        console.error('Failed to create database', error)
        this.$toast.error(`${message}`)
      }
      container.loading = false
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
