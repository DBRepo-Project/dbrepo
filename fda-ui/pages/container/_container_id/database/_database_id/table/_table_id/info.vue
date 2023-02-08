<template>
  <div>
    <TableToolbar :selection="selection" />
    <v-card flat tile>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-table</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Table ID
              </v-list-item-title>
              <v-list-item-content>
                <span v-if="table && table.id">{{ table.id }}</span>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-xsmall" />
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Table Description
              </v-list-item-title>
              <v-list-item-content>
                <span v-if="table && table.description">{{ table.description }}</span>
                <v-skeleton-loader v-if="!table" type="text" />
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Table Creator
              </v-list-item-title>
              <v-list-item-content>
                <span v-if="table && table.creator">{{ formatCreator(table.creator) }} <span v-if="is_owner(table)" style="flex:none;">&nbsp;(you)</span></span>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Table Creation
              </v-list-item-title>
              <v-list-item-content>
                <span v-if="table && table.created">{{ createdUTC }}</span>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-small" />
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-rabbit</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Protocols
              </v-list-item-title>
              <v-list-item-content>
                AMQP, MQTT
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Exchange Name
              </v-list-item-title>
              <v-list-item-content>
                <pre v-if="database && database.exchange_name">{{ database.exchange_name }}</pre>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Queue Name
              </v-list-item-title>
              <v-list-item-content>
                <pre v-if="table && table.queue_name">{{ table.queue_name }}</pre>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Routing Key
              </v-list-item-title>
              <v-list-item-content>
                <pre v-if="table && table.routing_key">{{ table.routing_key }}</pre>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title v-if="hasReadAccess" class="mt-2">
                Consumer Count
              </v-list-item-title>
              <v-list-item-content v-if="hasReadAccess" class="amqp-consumer">
                <span v-text="`${consumersUp}/${consumersTotal}`" />
                <v-badge
                  class="ml-1"
                  :color="consumersState.color"
                  :content="consumersState.text" />
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableToolbar from '@/components/TableToolbar'
import { formatTimestampUTCLabel, formatUser } from '@/utils'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      loadingConsumers: false,
      selection: [],
      consumers: [],
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`, activeClass: '' },
        { text: 'Tables', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, activeClass: '' },
        { text: `${this.$route.params.table_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, activeClass: '' }
      ],
      headers: [],
      dateColumns: []
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {
          headers: {},
          progress: false
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
    },
    hasReadAccess () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        /* database is public */
        return true
      }
      if (!this.user) {
        return false
      }
      if (this.database.creator.username === this.user.username) {
        /* user is creator of database */
        return true
      }
      if (!this.access) {
        return false
      }
      if (this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all') {
        /* user has some level of access */
        return true
      }
      return false
    },
    createdUTC () {
      if (this.table.created === undefined || this.table.created === null) {
        return null
      }
      return formatTimestampUTCLabel(this.table.created)
    },
    access () {
      return this.$store.state.access
    },
    consumersState () {
      if (this.consumersTotal === 0 || this.consumersTotal - this.consumersUp > 0 || this.loadingConsumers) {
        return { color: 'warning', text: 'pending' }
      }
      if (this.consumersTotal === 0) {
        return { color: 'error', text: 'down' }
      }
      return { color: 'success', text: 'up' }
    },
    consumersTotal () {
      if (this.loadingConsumers) {
        return 0
      }
      return this.consumers.length
    },
    consumersUp () {
      if (this.loadingConsumers) {
        return 0
      }
      return this.consumers.filter(c => c.active).length
    },
    canModify () {
      if (!this.token || !this.user.username) {
        /* not yet loaded */
        return false
      }
      return this.table.creator.username === this.user.username
    },
    versionColor () {
      if (this.version === null) {
        return 'secondary white--text'
      }
      return 'primary white--text'
    },
    versionFormatted () {
      if (this.version === null) {
        return null
      }
      return this.version + ' (UTC)'
    },
    versionISO () {
      if (this.version === null) {
        return null
      }
      return this.version.substring(0, 10) + 'T' + this.version.substring(11, 19) + 'Z'
    },
    brokerConfig () {
      return {
        headers: { Authorization: 'Basic ' + btoa(`${this.$config.brokerUsername}:${this.$config.brokerPassword}`) },
        progress: false
      }
    }
  },
  mounted () {
    this.pollConsumerStatus(true)
    setInterval(() => this.pollConsumerStatus(false), 5 * 1000)
  },
  methods: {
    formatCreator (creator) {
      return formatUser(creator)
    },
    is_owner (table) {
      if (!this.user) {
        return false
      }
      return table.creator.username === this.user.username
    },
    async pollConsumerStatus (first) {
      if (this.table === null || this.table.queue_name === null) {
        return
      }
      try {
        this.loadingConsumers = first
        const res = await this.$axios.get('/api/broker/consumers/%2F', this.brokerConfig)
        const consumers = res.data.filter(c => c.queue.name === this.table.queue_name)
        console.debug('filtered', consumers)
        this.consumers = consumers
      } catch (err) {
        console.error('Could not find consumers', err)
      }
      this.loadingConsumers = false
    }
  }
}
</script>
<style>
.v-card__text {
  font-size: initial;
}
.skeleton-large .v-skeleton-loader__text {
  width: 400px;
}
.skeleton-medium .v-skeleton-loader__text {
  width: 200px;
}
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
.skeleton-xsmall .v-skeleton-loader__text {
  width: 50px;
}
</style>
