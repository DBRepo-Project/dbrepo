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
              </v-list-item-content>
              <v-list-item-title v-if="hasDescription" class="mt-2">
                Table Description
              </v-list-item-title>
              <v-list-item-content v-if="hasDescription" v-text="table.description" />
              <v-list-item-title class="mt-2">
                Table Owner
              </v-list-item-title>
              <v-list-item-content>
                <span v-if="table && table.creator">{{ formatCreator(table.owner) }} <span v-if="is_owner(table)" style="flex:none;">&nbsp;(you)</span></span>
              </v-list-item-content>
              <v-list-item-title v-if="table && table.created" class="mt-2">
                Table Creation
              </v-list-item-title>
              <v-list-item-content v-if="table && table.created">
                <span>{{ createdUTC }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="canWriteQueues">
            <v-list-item-icon>
              <v-icon>mdi-rabbit</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Protocol
              </v-list-item-title>
              <v-list-item-content>
                AMQP
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Exchange
              </v-list-item-title>
              <v-list-item-content v-if="database">
                <span>
                  <v-badge inline :content="database.exchange_type" color="primary">{{ database.exchange_name }}</v-badge>
                </span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Queue
              </v-list-item-title>
              <v-list-item-content v-if="table">
                <span>
                  <v-badge inline :content="table.queue_type" color="primary">{{ table.queue_name }}</v-badge>
                </span>
              </v-list-item-content>
              <v-list-item-title v-if="table && table.routing_key" class="mt-2">
                Routing Key
              </v-list-item-title>
              <v-list-item-content v-if="table && table.routing_key">
                <pre v-text="table.routing_key" />
              </v-list-item-content>
              <v-list-item-title v-if="canRead" class="mt-2">
                Connection String
              </v-list-item-title>
              <v-list-item-content>
                <pre v-text="amqpString" />
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
import TableToolbar from '@/components/TableToolbar.vue'
import { formatTimestampUTCLabel } from '@/utils'
import UserMapper from '@/api/user.mapper'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      selection: [],
      consumers: [],
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}/info`, activeClass: '' },
        { text: 'Tables', to: `/database/${this.$route.params.database_id}/table`, activeClass: '' },
        { text: `${this.$route.params.table_id}`, to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, activeClass: '' }
      ],
      headers: [],
      dateColumns: [],
      loadingConsumers: false,
      loadingExchange: false,
      loadingQueue: false,
      exchange: null,
      queue: null
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
    },
    roles () {
      return this.$store.state.roles
    },
    canRead () {
      if (this.database && this.database.is_public) {
        return true
      }
      if (!this.user || !this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
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
    hasDescription () {
      return this.table && this.table.description !== null
    },
    canWriteQueues () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
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
    amqpString () {
      if (!this.user) {
        return null
      }
      return `amqp://${window.location.hostname}:5672/dbrepo (username=${this.user.username}, password=yourpassword)`
    }
  },
  methods: {
    formatCreator (creator) {
      return UserMapper.userToFullName(creator)
    },
    is_owner (table) {
      if (!this.user) {
        return false
      }
      return table.owner.id === this.user.id
    }
  }
}
</script>
<style>
.v-card__text {
  font-size: initial;
}
</style>
