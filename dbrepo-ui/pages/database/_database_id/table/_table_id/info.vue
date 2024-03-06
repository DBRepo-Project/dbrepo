<template>
  <div>
    <TableToolbar :selection="selection" />
    <v-card flat tile>
      <Summary v-if="hasIdentifier" :identifier="identifier" />
      <v-card-text v-if="hasIdentifier">
        <Select :identifiers="identifiers" :identifier="identifier" />
      </v-card-text>
    </v-card>
    <v-divider v-if="table && identifier" />
    <v-card flat tile>
      <v-card-title>Table</v-card-title>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title>
                Table ID
              </v-list-item-title>
              <v-list-item-content v-if="table && table.id" v-text="table.id" />
              <v-list-item-title v-if="table && table.data_length">
                Table Size
              </v-list-item-title>
              <v-list-item-content v-if="table && table.data_length" v-text="sizeToHumanLabel(table.data_length)" />
              <v-list-item-title v-if="table && table.num_rows">
                Table Rows
              </v-list-item-title>
              <v-list-item-content v-if="table && table.num_rows" v-text="table.num_rows" />
              <v-list-item-title v-if="hasDescription" class="mt-2">
                Table Description
              </v-list-item-title>
              <v-list-item-content v-if="hasDescription" v-text="table.description" />
              <v-list-item-title class="mt-2">
                Table Owner
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!table" type="text" class="skeleton-small" />
                <UserBadge v-if="table" :user="table.creator" :other-user="user" />
              </v-list-item-content>
              <v-list-item-title v-if="table && table.created" class="mt-2">
                Table Creation
              </v-list-item-title>
              <v-list-item-content v-if="table && table.created">
                <span>{{ createdUTC }}</span>
              </v-list-item-content>
              <v-list-item-title v-if="access && access.type" class="mt-2">
                Table Access
              </v-list-item-title>
              <v-list-item-content v-if="access && access.type">
                <span>
                  <v-badge v-if="brokerExtraInfo" inline :content="brokerExtraInfo" color="secondary">
                    <span v-text="accessDescription.text" />
                  </v-badge>
                  <span v-else v-text="accessDescription.text" />
                </span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
      <v-divider v-if="canWrite && canWriteQueues" />
      <v-card-title v-if="canWrite && canWriteQueues">Broker</v-card-title>
      <v-card-text v-if="canWrite && canWriteQueues">
        <v-list dense>
          <v-list-item>
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
                  <v-badge inline :content="database.exchange_type" color="code">{{ database.exchange_name }}</v-badge>
                </span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Queue
              </v-list-item-title>
              <v-list-item-content v-if="table">
                <span>
                  <v-badge inline :content="table.queue_type" color="code">{{ table.queue_name }}</v-badge>
                </span>
              </v-list-item-content>
              <v-list-item-title v-if="table && table.routing_key" class="mt-2">
                Routing Key
              </v-list-item-title>
              <v-list-item-content v-if="table && table.routing_key">
                <pre v-text="table.routing_key" />
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Connection String
              </v-list-item-title>
              <v-list-item-content>
                <span v-for="(port, i) in brokerPorts" :key="i">
                  <pre v-if="![5671,5672].includes(port)" class="pb-1" v-text="amqpString" />
                  <v-badge inline :content="amqpBadgeText(port)" :color="amqpBadgeColor(port)">
                    <pre class="pb-1" v-text="amqpString(port)" />
                  </v-badge>
                </span>
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
import TableToolbar from '@/components/table/TableToolbar.vue'
import Select from '@/components/identifier/Select'
import Summary from '@/components/identifier/Summary'
import { formatTimestampUTCLabel, sizeToHumanLabel } from '@/utils'
import UserBadge from '@/components/UserBadge.vue'

export default {
  components: {
    Summary,
    Select,
    TableToolbar,
    UserBadge
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
    pid () {
      return this.$route.query.pid
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
      if (this.database && this.database.is_public) {
        return true
      }
      if (!this.user || !this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
    },
    canWrite () {
      if (!this.table || !this.user || !this.access) {
        return false
      }
      return (this.access.type === 'write_own' && this.table.owner.id === this.user.id) || this.access.type === 'write_all'
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
    identifiers () {
      if (!this.table.identifiers || this.table.identifiers.length === 0) {
        return []
      }
      return this.table.identifiers
    },
    identifier () {
      if (this.pid) {
        const filter = this.identifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.identifiers[0]
    },
    hasIdentifier () {
      return this.identifiers.length > 0
    },
    brokerExtraInfo () {
      return this.$config.brokerExtraInfo
    },
    brokerHost () {
      return this.$config.brokerHost
    },
    brokerPorts () {
      return this.$config.brokerPorts
    },
    accessDescription () {
      if (!this.access) {
        return
      }
      if (this.canWrite) {
        return { text: 'You can write to this table' }
      } else if (this.canRead) {
        return { text: 'You can read all contents of this table' }
      } else {
        return { text: null }
      }
    }
  },
  methods: {
    sizeToHumanLabel,
    amqpBadgeText (port) {
      if (port === 5672) {
        return 'insecure'
      } else if (port === 5671) {
        return 'secure'
      }
      return null
    },
    amqpBadgeColor (port) {
      if (port === 5672) {
        return 'warning'
      } else if (port === 5671) {
        return 'success'
      }
      return null
    },
    amqpString (port) {
      if (!this.user) {
        return null
      }
      return `amqp://${this.brokerHost}:${port}/dbrepo (username=${this.user.username}, password=yourpassword)`
    }
  }
}
</script>
<style>
.v-card__text {
  font-size: initial;
}
.skeleton-large > div {
  width: 400px !important;
}
.skeleton-medium > div {
  width: 200px !important;
}
.skeleton-small > div {
  width: 100px !important;
}
.skeleton-xsmall > div {
  width: 50px !important;
}
</style>
