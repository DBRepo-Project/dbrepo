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
                Table Owner
              </v-list-item-title>
              <v-list-item-content>
                <span v-if="table && table.creator">{{ formatCreator(table.owner) }} <span v-if="is_owner(table)" style="flex:none;">&nbsp;(you)</span></span>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title v-if="table && table.created" class="mt-2">
                Table Creation
              </v-list-item-title>
              <v-list-item-content v-if="table && table.created">
                <span>{{ createdUTC }}</span>
              </v-list-item-content>
              <v-list-item-content v-if="!table">
                <v-skeleton-loader type="text" class="skeleton-medium" />
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="canWriteQueues">
            <v-list-item-icon>
              <v-icon>mdi-rabbit</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>
                Exchange Type
              </v-list-item-title>
              <v-list-item-content>
                Direct (AMQP)
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Exchange Name
              </v-list-item-title>
              <v-list-item-content>
                <pre v-if="database && database.exchange_name">{{ database.exchange_name }}</pre>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title v-if="table && table.queue_name" class="mt-2">
                Queue Name
              </v-list-item-title>
              <v-list-item-content v-if="table && table.queue_name">
                <pre>{{ table.queue_name }}</pre>
              </v-list-item-content>
              <v-list-item-content v-if="!table">
                <v-skeleton-loader type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title v-if="table && table.routing_key" class="mt-2">
                Routing Key
              </v-list-item-title>
              <v-list-item-content v-if="table && table.routing_key">
                <pre>{{ table.routing_key }}</pre>
              </v-list-item-content>
              <v-list-item-content v-if="!table">
                <v-skeleton-loader type="text" class="skeleton-medium" />
              </v-list-item-content>
              <v-list-item-title v-if="canRead" class="mt-2">
                Consumer Count
              </v-list-item-title>
              <v-list-item-content v-if="canWriteQueues" class="amqp-consumer">
                <span v-text="`${consumersUp}/${consumersTotal}`" />
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
import BrokerService from '@/api/broker.service'
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
      dateColumns: []
    }
  },
  computed: {
    token () {
      return this.$store.state.token
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
    roles () {
      return this.$store.state.roles
    },
    canRead () {
      if (this.database?.is_public) {
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
    consumersTotal () {
      return this.consumers.length
    },
    consumersUp () {
      return this.consumers.filter(c => c.active).length
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
    }
  },
  watch: {
    table () {
      this.consumerDetails()
    }
  },
  mounted () {
    this.consumerDetails()
  },
  methods: {
    formatCreator (creator) {
      return UserMapper.userToFullName(creator)
    },
    is_owner (table) {
      if (!this.user) {
        return false
      }
      return table.owner.username === this.user.username
    },
    consumerDetails () {
      if (!this.table) {
        return
      }
      this.loadingConsumers = true
      BrokerService.findConsumers()
        .then((consumers) => {
          this.consumers = consumers.filter(c => c.queue.name === this.table.queue_name)
        })
        .finally(() => {
          this.loadingConsumers = false
        })
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
