<template>
  <div>
    <v-progress-linear v-if="loadingContainers || loadingDatabases" :indeterminate="!error" />
    <v-card v-if="!$vuetify.theme.dark && containers.length> 0" flat tile>
      <v-divider class="mx-4" />
    </v-card>
    <v-card
      v-for="(container, idx) in containers"
      :key="idx"
      :to="link(container)"
      flat
      tile>
      <v-divider v-if="idx !== 0" class="mx-4" />
      <v-card-title v-if="!hasDatabase(container)" v-text="container.name" />
      <v-card-title v-if="hasDatabase(container)">
        <a :href="`/container/${container.id}/database/${container.database.id}`">{{ container.name }}</a>
      </v-card-title>
      <v-card-subtitle class="db-subtitle" v-text="formatCreators(container)" />
      <v-card-text v-if="hasDatabase(container)" class="db-description">
        <div class="db-tags">
          <v-chip v-if="hasDatabase(container) && container.database.is_public" small color="green" outlined>Public</v-chip>
          <v-chip v-if="hasDatabase(container) && !container.database.is_public" small color="red" outlined>Private</v-chip>
          <v-chip
            v-if="identifierYear(container)"
            small
            outlined
            v-text="identifierYear(container)" />
          <v-chip
            v-if="hasIdentifier(container)"
            small
            outlined
            v-text="container.database.identifier.publisher" />
        </div>
        <div v-text="identifierDescription(container)" />
        <v-btn
          v-if="needsDatabase(container)"
          small
          secondary
          :loading="container?.loading"
          @click.stop="createDatabase(container)">
          Create Database
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
import DatabaseService from '@/api/database.service'
import ContainerService from '@/api/container.service'
import ContainerMapper from '@/api/container.mapper'

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
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    }
  },
  mounted () {
    this.loadContainers()
  },
  methods: {
    formatCreators (container) {
      return ContainerMapper.containerToCreator(container)
    },
    needsDatabase (container) {
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
    identifierYear (container) {
      if (!container || !container.database || !container.database.identifier || !container.database.identifier.publication_year) {
        return null
      }
      return container.database.identifier.publication_year
    },
    identifierDescription (container) {
      if (!container || !container.database || !container.database.identifier) {
        return null
      }
      return container.database.identifier.description
    },
    loadContainers () {
      this.createDbDialog = false
      this.loadingContainers = true
      ContainerService.findAll(this.limit)
        .then((containers) => {
          this.containers = containers
          console.info('Found', this.containers.length, 'container(s)')
        })
      this.loadingContainers = false
    },
    createDatabase (container) {
      container.loading = true
      DatabaseService.create(container.id, { name: container.name, is_public: true })
        .then((database) => {
          container.loading = false
          this.$router.push(`/container/${container.id}/database/${database.id}`)
        })
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
