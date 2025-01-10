<template>
  <div
    v-if="table">
    <TableToolbar
      :selection="selection" />
    <v-card
      variant="flat">
      <Summary
        v-if="hasIdentifier"
        :identifier="identifier" />
      <v-card-text
        v-if="hasIdentifier">
        <Select
          :identifiers="identifiers"
          :identifier="identifier" />
      </v-card-text>
    </v-card>
    <v-divider
      v-if="identifier" />
    <v-card
      variant="flat"
      rounded="0"
      :title="$t('pages.table.title')">
      <v-card-text>
        <v-list
          dense>
          <v-list-item
            :title="$t('pages.table.name.title')">
            {{ table.internal_name }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.size.title')">
            {{ sizeToHumanLabel(table.data_length) }}
          </v-list-item>
          <v-list-item
            v-if="canRead && table.num_rows"
            :title="$t('pages.table.rows.title')">
            {{ table.num_rows }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.description.title')">
            {{ hasDescription ? table.description : $t('pages.table.description.empty') }}
          </v-list-item>
          <v-list-item
            v-if="accessDescription"
            :title="$t('pages.database.subpages.access.title')">
            <span>
              <v-badge
                v-if="brokerExtraInfo"
                inline
                color="secondary"
                :content="brokerExtraInfo">
                <span>
                  {{ accessDescription }}
                </span>
              </v-badge>
              <span
                v-else>
                {{ accessDescription }}
              </span>
            </span>
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.owner.title')">
            <UserBadge
              :user="table.owner"
              :other-user="user" />
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <v-divider
      v-if="canWrite && canWriteQueues" />
    <v-card
      v-if="canWrite && canWriteQueues"
      variant="flat"
      rounded="0"
      :title="$t('pages.table.broker.title')">
      <v-card-text>
        <v-list
          dense>
          <v-list-item
            :title="$t('pages.table.connection.title')">
            <p
              v-for="(connection, i) in brokerConnections"
              :key="`p-${i}`">
              <pre
                v-if="!connection.encrypted"
                class="pb-1">{{ connection.value }}</pre>
              <v-badge
                v-else
                inline
                :content="$t('pages.table.connection.secure')"
                color="success">
              <pre
                class="pb-1">{{ connection.value }}</pre>
              </v-badge>
            </p>
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.exchange.title')">
            {{ database.exchange_name }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.queue.title')">
            {{ table.queue_name }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.routing-key.title')">
            <div v-if="table.routing_key">
              <pre>{{ table.routing_key }}</pre>
            </div>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import TableToolbar from '@/components/table/TableToolbar.vue'
import Select from '@/components/identifier/Select.vue'
import Summary from '@/components/identifier/Summary.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    Summary,
    Select,
    TableToolbar,
    UserBadge
  },
  setup () {
    const config = useRuntimeConfig()
    const userStore = useUserStore()
    const { database_id, table_id } = useRoute().params
    const { error, data } = useFetch(`${config.public.api.server}/api/database/${database_id}/table/${table_id}`, {
      immediate: true,
      timeout: 90_000,
      headers: {
        Accept: 'application/json',
        Authorization: userStore.getToken ? `Bearer ${userStore.getToken}` : null
      }
    })
    if (data.value) {
      const identifierService = useIdentifierService()
      useServerHead(identifierService.databaseToServerHead(data.value))
      useServerSeoMeta(identifierService.databaseToServerSeoMeta(data.value))
    }
    return {
      table: data,
      error
    }
  },
  data () {
    return {
      selection: [],
      consumers: [],
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`
        },
        {
          title: this.$t('navigation.info'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/info`,
          disabled: true
        }
      ],
      headers: [],
      dateColumns: [],
      loading: false,
      exchange: null,
      queue: null,
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    pid () {
      return this.$route.query.pid
    },
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    roles () {
      return this.userStore.getRoles
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
      return (this.access.type === 'write_own' && this.table.owned_by === this.user.id) || this.access.type === 'write_all'
    },
    access () {
      return this.userStore.getAccess
    },
    hasDescription () {
      return this.table && this.table.description
    },
    canWriteQueues () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    identifiers () {
      if (!this.table || !this.table.identifiers || this.table.identifiers.length === 0) {
        return []
      }
      return this.table.identifiers
    },
    filteredIdentifiers () {
      if (!this.identifiers) {
        return []
      }
      if (!this.user) {
        return this.identifiers.filter(i => i.status === 'published')
      }
      return this.identifiers.filter(i => i.status === 'published' || i.owned_by === this.user.id)
    },
    identifier () {
      if (this.pid) {
        const filter = this.filteredIdentifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.filteredIdentifiers[0]
    },
    hasIdentifier () {
      return this.identifier
    },
    brokerExtraInfo () {
      return this.$config.public.broker.extra
    },
    brokerConnections () {
      if (!this.$config.public.broker.connections) {
        return []
      }
      return this.$config.public.broker.connections.split(',').map(c => {
        if (c.startsWith('^')) {
          return {
            encrypted: true,
            value: c.substring(1)
          }
        }
        return {
          encrypted: false,
          value: c
        }
      })
    },
    accessDescription () {
      if (!this.access) {
        return null
      }
      if (this.canWrite) {
        return this.$t('pages.table.connection.permissions.write')
      } else if (this.canRead) {
        return this.$t('pages.table.connection.permissions.read')
      }
    }
  }
}
</script>
