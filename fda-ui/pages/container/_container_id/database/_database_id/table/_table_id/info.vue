<template>
  <div>
    <TableToolbar :table="table" :selection="selection" />
    <v-card flat tile>
      <v-list>
        <v-list-item>
          <v-list dense>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>
                  Table ID
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ table.id }}</span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-xsmall" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>
                  Table Description
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ table.description }}</span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>
                  Exchange Name (AMQP/MQTT)
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ database.exchange_name }}</span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="table.queue_name">
              <v-list-item-content>
                <v-list-item-title>
                  Queue Name (AMQP/MQTT)
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ table.queue_name }}</span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="table.routing_key">
              <v-list-item-content>
                <v-list-item-title>
                  Routing Key (AMQP/MQTT)
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ table.routing_key }}</span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="hasReadAccess">
              <v-list-item-content>
                <v-list-item-title>
                  AMQP Consumer(s)
                </v-list-item-title>
                <v-list-item-content class="amqp-consumer">
                  <span v-if="justCreated">Creating consumers ...</span>
                  <v-skeleton-loader v-if="loadingConsumers" type="text" class="skeleton-small" />
                  <div v-if="!loadingConsumers">
                    <span v-if="!justCreated" v-text="`${consumersUp}/${consumersTotal}`" />
                    <v-badge
                      v-if="!justCreated"
                      class="ml-1"
                      :color="consumersState.color"
                      :content="consumersState.text" />
                  </div>
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>
                  Table Creator
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ formatCreator(table.creator) }} <span v-if="is_owner(table)" style="flex:none;">&nbsp;(you)</span></span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="createdUTC">
              <v-list-item-content>
                <v-list-item-title>
                  Table Creation
                </v-list-item-title>
                <v-list-item-content>
                  <span v-if="!loading">{{ createdUTC }}</span>
                  <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                </v-list-item-content>
              </v-list-item-content>
            </v-list-item>
          </v-list>
        </v-list-item>
      </v-list>
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
      loading: true,
      loadingConsumers: false,
      selection: [],
      consumers: [],
      database: {
        exchange_name: null,
        creator: {
          username: null
        },
        created: null
      },
      table: {
        name: null,
        description: null,
        columns: [],
        creator: {
          username: null
        }
      },
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
    justCreated () {
      return new Date().getTime() - new Date(this.table.created).getTime() <= 60000
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
      if (this.consumersTotal === 0) {
        return { color: 'error', text: 'down' }
      }
      if (this.consumersTotal - this.consumersUp > 0) {
        return { color: 'warning', text: 'up' }
      }
      return { color: 'success', text: 'up' }
    },
    consumersTotal () {
      return this.consumers.length
    },
    consumersUp () {
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
    this.loadDatabase()
    this.loadTable()
      .then(() => this.pollConsumerStatus(true))
    setInterval(() => this.pollConsumerStatus(false), 5000)
  },
  methods: {
    async loadTable () {
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, this.config)
        this.table = res.data
        console.debug('table', this.table)
      } catch (error) {
        console.error('Failed to load table', error)
        const { message } = error.response
        this.$toast.error('Failed to load table: ' + message)
      }
      this.loading = false
    },
    async loadDatabase () {
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', this.table)
      } catch (error) {
        console.error('Failed to load database', error)
        const { message } = error.response
        this.$toast.error('Failed to load database: ' + message)
      }
      this.loading = false
    },
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
