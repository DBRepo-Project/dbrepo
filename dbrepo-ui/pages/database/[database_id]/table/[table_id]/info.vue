<template>
  <div>
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
        <v-skeleton-loader
          v-if="!cachedTable"
          type="list-item-three-line"
          width="50%" />
        <v-list
          v-if="cachedTable"
          dense>
          <v-list-item
            :title="$t('pages.table.id.title')">
            {{ cachedTable.id }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.name.title')">
            {{ cachedTable.internal_name }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.visibility.title')">
            {{ databaseVisibility }}
          </v-list-item>
          <v-list-item
            v-if="table"
            :title="$t('pages.table.size.title')">
            {{ sizeToHumanLabel(table.data_length) }}
          </v-list-item>
          <v-list-item
            v-if="table"
            :title="$t('pages.table.result-rows.title')">
            {{ table.num_rows }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.description.title')">
            {{ hasDescription ? cachedTable.description : $t('pages.table.description.empty') }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.table.owner.title')">
            <UserBadge
              v-if="table"
              :user="table.owner"
              :other-user="user" />
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
        </v-list>
      </v-card-text>
    </v-card>
    <v-divider />
    <v-card
      :title="$t('pages.database.title')"
      variant="flat">
      <v-card-text>
        <v-list dense>
          <v-list-item
            v-if="database"
            :title="$t('pages.database.name.title')">
            <NuxtLink
              class="text-primary"
              :to="`/database/${$route.params.database_id}`">
              {{ database.internal_name }}
            </NuxtLink>
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

<script setup>
const config = useRuntimeConfig()
const { database_id, table_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.server}/api/database/${database_id}/table/${table_id}`)
if (data.value) {
  const identifierService = useIdentifierService()
  useServerHead(identifierService.tableToServerHead(data.value))
  useServerSeoMeta(identifierService.tableToServerSeoMeta(data.value))
}
</script>
<script>
import TableToolbar from '@/components/table/TableToolbar.vue'
import Select from '@/components/identifier/Select.vue'
import Summary from '@/components/identifier/Summary.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { formatTimestampUTCLabel, sizeToHumanLabel } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
      table: null,
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
    cachedTable () {
      return this.cacheStore.getTable
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
      return (this.access.type === 'write_own' && this.cachedTable.owned_by === this.user.id) || this.access.type === 'write_all'
    },
    access () {
      return this.userStore.getAccess
    },
    hasDescription () {
      return this.table && this.cachedTable.description
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
    },
    databaseVisibility () {
      if (!this.database) {
        return null
      }
      if (this.database.is_public && this.cachedTable.is_schema_public) {
        return this.$t('pages.table.visibility.open')
      }
      if (!this.database.is_public && !this.cachedTable.is_schema_public) {
        return this.$t('pages.table.visibility.closed')
      }
      return this.database.is_public ? this.$t('pages.database.visibility.data') : this.$t('pages.database.visibility.schema')
    }
  },
  mounted () {
    this.fetchTable()
  },
  methods: {
    fetchTable () {
      this.loading = true
      const tableService = useTableService()
      tableService.findOne(this.$route.params.database_id, this.$route.params.table_id)
        .then((table) => {
          this.loading = false
          this.table = table
        })
        .catch(({code}) => {
          this.loading = false
          const toast = useToastInstance()
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
