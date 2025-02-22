<template>
  <div
    v-if="identifier || canViewInfo">
    <TableToolbar
      :selection="selection" />
    <v-card
      v-if="identifier"
      variant="flat">
      <Summary
        :identifier="identifier" />
      <v-card-text>
        <Select
          :identifiers="identifiers"
          :identifier="identifier" />
      </v-card-text>
    </v-card>
    <v-divider
      v-if="identifier" />
    <v-card
      v-if="canViewInfo"
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
              :other-user="cacheUser" />
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
import { ref } from 'vue'

const config = useRuntimeConfig()
const { pid } = useRoute().query
const { database_id, table_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.client}/api/identifier?dbid=${database_id}&tid=${table_id}&type=table&status=published`)

if (data.value && data.value.length > 0) {
  const identifierService = useIdentifierService()
  useServerHead(identifierService.identifiersToServerHead(data.value))
  useServerSeoMeta(identifierService.identifiersToServerSeoMeta(data.value))
}
const identifier = ref(data.value && data.value.length > 0 ? (pid && data.value.filter(i => i.id === pid).length > 0 ? data.value.filter(i => i.id === pid)[0] : data.value[0]) : null)

const cacheStore = useCacheStore()
cacheStore.setIdentifier(identifier)
</script>
<script>
import TableToolbar from '@/components/table/TableToolbar.vue'
import Select from '@/components/identifier/Select.vue'
import Summary from '@/components/identifier/Summary.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { useCacheStore } from '@/stores/cache.js'

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
      cacheStore: useCacheStore()
    }
  },
  computed: {
    pid () {
      return this.$route.query.pid
    },
    database () {
      return this.cacheStore.getDatabase
    },
    table () {
      return this.cacheStore.getTable
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    roles () {
      return this.cacheStore.getRoles
    },
    canRead () {
      if (this.database && this.database.is_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    canViewInfo () {
      if (!this.table) {
        return false
      }
      if (this.table.is_public || this.table.is_schema_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    canWrite () {
      const userService = useUserService()
      return userService.hasWriteAccess(this.table, this.access, this.cacheUser)
    },
    access () {
      return this.cacheStore.getAccess
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
      if (!this.table || !this.table.identifiers) {
        return []
      }
      return this.table.identifiers.filter(i => i.query_id === this.$route.params.subset_id)
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
