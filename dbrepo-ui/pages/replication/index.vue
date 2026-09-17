<template>
  <div class="replication-page">
    <div class="d-flex align-center mb-6">
      <div>
        <h1 class="text-h4">{{ $t('replication.title') }}</h1>
        <div
          v-if="status"
          class="text-medium-emphasis mt-1">
          {{ $t('replication.updated', {time: formatDate(lastUpdated)}) }}
        </div>
      </div>
      <v-spacer />
      <v-btn
        icon="mdi-refresh"
        variant="text"
        :loading="loading"
        @click="refreshAll">
        <v-tooltip activator="parent">{{ $t('replication.actions.refresh') }}</v-tooltip>
      </v-btn>
    </div>

    <v-alert
      v-if="!canManageReplication"
      type="error"
      variant="tonal"
      :title="$t('replication.forbidden')" />

    <template v-else>
      <section aria-labelledby="replication-health">
        <div class="d-flex align-center mb-3">
          <h2
            id="replication-health"
            class="text-h6">
            {{ $t('replication.health.title') }}
          </h2>
          <v-spacer />
          <v-chip
            v-if="status"
            size="small"
            variant="flat"
            :color="statusColor(status.health.status)">
            {{ status.health.status }}
          </v-chip>
        </div>
        <v-row>
          <v-col
            v-for="service in healthServices"
            :key="service.key"
            cols="12"
            sm="6"
            lg="3">
            <v-sheet
              border
              class="pa-4 fill-height"
              rounded="sm">
              <div class="d-flex align-center">
                <v-icon
                  :icon="service.icon"
                  class="mr-3"
                  :color="statusColor(service.value?.status)" />
                <div class="text-subtitle-1 font-weight-medium">{{ service.label }}</div>
                <v-spacer />
                <v-chip
                  size="x-small"
                  variant="tonal"
                  :color="statusColor(service.value?.status)">
                  {{ service.value?.status || $t('replication.health.unknown') }}
                </v-chip>
              </div>
              <div class="text-body-2 text-medium-emphasis mt-3">
                <span v-if="service.value?.duration_ms !== null && service.value?.duration_ms !== undefined">
                  {{ service.value.duration_ms }} ms
                </span>
                <span v-else>{{ service.value?.error || $t('replication.health.noResponse') }}</span>
              </div>
            </v-sheet>
          </v-col>
        </v-row>
      </section>

      <section
        class="mt-8"
        aria-labelledby="replication-backlog">
        <h2
          id="replication-backlog"
          class="text-h6 mb-3">
          {{ $t('replication.backlog.title') }}
        </h2>
        <v-table
          density="comfortable"
          class="border-sm rounded-sm">
          <thead>
            <tr>
              <th>{{ $t('replication.columns.source') }}</th>
              <th class="text-end">{{ $t('replication.columns.total') }}</th>
              <th class="text-end">{{ $t('replication.columns.pending') }}</th>
              <th class="text-end">{{ $t('replication.columns.failed') }}</th>
              <th class="text-end">{{ $t('replication.columns.succeeded') }}</th>
              <th>{{ $t('replication.columns.nextAttempt') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="outbox in outboxSummaries"
              :key="outbox.key">
              <td>{{ outbox.label }}</td>
              <td class="text-end">{{ outbox.value?.total ?? '-' }}</td>
              <td class="text-end">{{ outbox.value?.pending ?? '-' }}</td>
              <td class="text-end">
                <v-chip
                  v-if="outbox.value"
                  size="x-small"
                  variant="tonal"
                  :color="outbox.value.failed > 0 ? 'error' : 'success'">
                  {{ outbox.value.failed }}
                </v-chip>
              </td>
              <td class="text-end">{{ outbox.value?.succeeded ?? '-' }}</td>
              <td>
                <span v-if="outbox.value?.available">{{ formatDate(outbox.value.next_attempt_at) }}</span>
                <span
                  v-else
                  class="text-error">{{ outbox.value?.error || $t('replication.health.unavailable') }}</span>
              </td>
            </tr>
          </tbody>
        </v-table>
      </section>

      <section
        class="mt-8"
        aria-labelledby="replication-sync">
        <h2
          id="replication-sync"
          class="text-h6 mb-3">
          {{ $t('replication.synchronisation.title') }}
        </h2>
        <v-row align="start">
          <v-col
            cols="12"
            md="5">
            <v-select
              v-model="selectedDatabaseId"
              :items="replicatedDatabases"
              item-title="name"
              item-value="id"
              :label="$t('replication.synchronisation.database')"
              variant="outlined"
              hide-details="auto"
              :loading="loadingDatabases" />
          </v-col>
          <v-col
            cols="12"
            md="4">
            <v-select
              v-model="selectedTableId"
              :items="selectedDatabase?.tables || []"
              item-title="name"
              item-value="id"
              clearable
              :disabled="!selectedDatabaseId"
              :label="$t('replication.synchronisation.table')"
              variant="outlined"
              hide-details="auto" />
          </v-col>
          <v-col
            cols="12"
            md="3">
            <v-text-field
              v-model.number="pageSize"
              type="number"
              min="1"
              max="1000"
              :label="$t('replication.synchronisation.pageSize')"
              variant="outlined"
              hide-details="auto" />
          </v-col>
        </v-row>
        <div class="d-flex flex-wrap ga-2 mt-4">
          <v-btn
            color="primary"
            prepend-icon="mdi-database-sync"
            :disabled="!selectedDatabaseId || !validPageSize"
            :loading="synchronisingDatabase"
            @click="synchroniseDatabase">
            {{ $t('replication.actions.synchroniseDatabase') }}
          </v-btn>
          <v-btn
            variant="outlined"
            prepend-icon="mdi-table-sync"
            :disabled="!selectedDatabaseId || !selectedTableId || !validPageSize"
            :loading="synchronisingTable"
            @click="synchroniseTable">
            {{ $t('replication.actions.synchroniseTable') }}
          </v-btn>
        </div>
        <v-alert
          v-if="synchronisationResult"
          class="mt-4"
          type="success"
          variant="tonal"
          closable
          @click:close="synchronisationResult = null">
          {{ synchronisationResult }}
        </v-alert>
      </section>

      <section
        class="mt-8"
        aria-labelledby="replication-outboxes">
        <div class="d-flex align-center mb-2">
          <h2
            id="replication-outboxes"
            class="text-h6">
            {{ $t('replication.outboxes.title') }}
          </h2>
          <v-spacer />
          <v-btn
            icon="mdi-replay"
            variant="text"
            :loading="retryingAll"
            :disabled="activeOutbox === 'data' && !selectedDatabaseId"
            @click="retryAll">
            <v-tooltip activator="parent">{{ $t('replication.actions.retryAll') }}</v-tooltip>
          </v-btn>
        </div>
        <v-tabs
          v-model="activeOutbox"
          color="primary">
          <v-tab value="replication">{{ $t('replication.sources.replication') }}</v-tab>
          <v-tab value="metadata">{{ $t('replication.sources.metadata') }}</v-tab>
          <v-tab value="data">{{ $t('replication.sources.data') }}</v-tab>
        </v-tabs>
        <v-window v-model="activeOutbox">
          <v-window-item value="replication">
            <v-data-table
              :headers="replicationHeaders"
              :items="replicationOutbox"
              :loading="loadingOutboxes"
              item-value="id"
              density="comfortable">
              <template #item.status="{item}">
                <v-chip size="x-small" variant="tonal" :color="statusColor(item.status)">{{ item.status }}</v-chip>
              </template>
              <template #item.nextAttemptAt="{item}">{{ formatDate(item.nextAttemptAt) }}</template>
              <template #item.actions="{item}">
                <v-btn icon="mdi-replay" size="small" variant="text" @click="retryEntry('replication', item.id)">
                  <v-tooltip activator="parent">{{ $t('replication.actions.retry') }}</v-tooltip>
                </v-btn>
              </template>
            </v-data-table>
          </v-window-item>
          <v-window-item value="metadata">
            <v-data-table
              :headers="metadataHeaders"
              :items="metadataOutbox"
              :loading="loadingOutboxes"
              item-value="id"
              density="comfortable">
              <template #item.status="{item}">
                <v-chip size="x-small" variant="tonal" :color="statusColor(item.status)">{{ item.status }}</v-chip>
              </template>
              <template #item.nextAttemptAt="{item}">{{ formatDate(item.nextAttemptAt) }}</template>
              <template #item.actions="{item}">
                <v-btn icon="mdi-replay" size="small" variant="text" @click="retryEntry('metadata', item.id)">
                  <v-tooltip activator="parent">{{ $t('replication.actions.retry') }}</v-tooltip>
                </v-btn>
              </template>
            </v-data-table>
          </v-window-item>
          <v-window-item value="data">
            <v-alert
              v-if="!selectedDatabaseId"
              class="mt-4"
              type="info"
              variant="tonal"
              :text="$t('replication.outboxes.selectDatabase')" />
            <v-data-table
              v-else
              :headers="dataHeaders"
              :items="dataOutbox"
              :loading="loadingOutboxes"
              item-value="id"
              density="comfortable">
              <template #item.status="{item}">
                <v-chip size="x-small" variant="tonal" :color="statusColor(item.status)">{{ item.status }}</v-chip>
              </template>
              <template #item.nextAttemptAt="{item}">{{ formatDate(item.nextAttemptAt) }}</template>
              <template #item.actions="{item}">
                <v-btn icon="mdi-replay" size="small" variant="text" @click="retryEntry('data', item.id)">
                  <v-tooltip activator="parent">{{ $t('replication.actions.retry') }}</v-tooltip>
                </v-btn>
              </template>
            </v-data-table>
          </v-window-item>
        </v-window>
      </section>
    </template>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      cacheStore: useCacheStore(),
      status: null,
      databases: [],
      selectedDatabase: null,
      selectedDatabaseId: null,
      selectedTableId: null,
      pageSize: 100,
      replicationOutbox: [],
      metadataOutbox: [],
      dataOutbox: [],
      activeOutbox: 'replication',
      synchronisationResult: null,
      loading: false,
      loadingDatabases: false,
      loadingOutboxes: false,
      synchronisingDatabase: false,
      synchronisingTable: false,
      retryingAll: false,
      lastUpdated: null
    }
  },
  computed: {
    roles () {
      return this.cacheStore.getRoles
    },
    canManageReplication () {
      return this.roles && this.roles.includes('system')
    },
    validPageSize () {
      return Number.isInteger(this.pageSize) && this.pageSize > 0 && this.pageSize <= 1000
    },
    replicatedDatabases () {
      return this.databases.filter(database => database.replica_urls && Object.keys(database.replica_urls).length > 0)
    },
    healthServices () {
      return [
        {key: 'replication', label: this.$t('replication.sources.replication'), icon: 'mdi-database-sync', value: this.status?.health?.replication_service},
        {key: 'metadata', label: this.$t('replication.sources.metadata'), icon: 'mdi-database-cog', value: this.status?.health?.metadata_service},
        {key: 'data', label: this.$t('replication.sources.data'), icon: 'mdi-database', value: this.status?.health?.data_service},
        {key: 'broker', label: this.$t('replication.sources.broker'), icon: 'mdi-message-processing', value: this.status?.health?.broker}
      ]
    },
    outboxSummaries () {
      return [
        {key: 'replication', label: this.$t('replication.sources.replication'), value: this.status?.outboxes?.replication_service},
        {key: 'metadata', label: this.$t('replication.sources.metadata'), value: this.status?.outboxes?.metadata_service},
        {key: 'data', label: this.$t('replication.sources.data'), value: this.status?.outboxes?.data_service}
      ]
    },
    replicationHeaders () {
      return [
        {title: this.$t('replication.columns.operation'), key: 'operationType'},
        {title: this.$t('replication.columns.target'), key: 'targetSiteUrl'},
        {title: this.$t('replication.columns.status'), key: 'status'},
        {title: this.$t('replication.columns.attempts'), key: 'attempts'},
        {title: this.$t('replication.columns.nextAttempt'), key: 'nextAttemptAt'},
        {title: '', key: 'actions', sortable: false, align: 'end'}
      ]
    },
    metadataHeaders () {
      return [
        {title: this.$t('replication.columns.operation'), key: 'notificationType'},
        {title: this.$t('replication.columns.path'), key: 'path'},
        {title: this.$t('replication.columns.status'), key: 'status'},
        {title: this.$t('replication.columns.attempts'), key: 'attempts'},
        {title: this.$t('replication.columns.nextAttempt'), key: 'nextAttemptAt'},
        {title: '', key: 'actions', sortable: false, align: 'end'}
      ]
    },
    dataHeaders () {
      return [
        {title: this.$t('replication.columns.method'), key: 'httpMethod'},
        {title: this.$t('replication.columns.table'), key: 'tableId'},
        {title: this.$t('replication.columns.status'), key: 'status'},
        {title: this.$t('replication.columns.attempts'), key: 'attempts'},
        {title: this.$t('replication.columns.nextAttempt'), key: 'nextAttemptAt'},
        {title: '', key: 'actions', sortable: false, align: 'end'}
      ]
    }
  },
  watch: {
    selectedDatabaseId (databaseId) {
      this.selectedTableId = null
      this.synchronisationResult = null
      this.dataOutbox = []
      if (!databaseId) {
        this.selectedDatabase = null
        return
      }
      const databaseService = useDatabaseService()
      databaseService.findOne(databaseId)
        .then(database => {
          this.selectedDatabase = database
          return this.loadDataOutbox()
        })
        .catch(error => this.showError(error))
    }
  },
  mounted () {
    if (this.canManageReplication) {
      this.refreshAll()
    }
  },
  methods: {
    statusColor (status) {
      if (['UP', 'SUCCEEDED', 'COMPLETED'].includes(status)) {
        return 'success'
      }
      if (['DOWN', 'FAILED'].includes(status)) {
        return 'error'
      }
      if (['DEGRADED', 'PENDING', 'PROCESSING'].includes(status)) {
        return 'warning'
      }
      return 'secondary'
    },
    formatDate (value) {
      if (!value) {
        return '-'
      }
      return new Intl.DateTimeFormat(undefined, {dateStyle: 'medium', timeStyle: 'medium'}).format(new Date(value))
    },
    showError (error) {
      const toast = useToastInstance()
      toast.error(error?.message || this.$t('replication.error'))
    },
    async refreshAll () {
      this.loading = true
      this.loadingDatabases = true
      this.loadingOutboxes = true
      const replicationService = useReplicationService()
      const databaseService = useDatabaseService()
      try {
        const [status, databases, replicationOutbox, metadataOutbox] = await Promise.all([
          replicationService.findStatus(),
          databaseService.findAll(),
          replicationService.findReplicationOutbox(),
          replicationService.findMetadataOutbox()
        ])
        this.status = status
        this.databases = databases
        this.replicationOutbox = replicationOutbox
        this.metadataOutbox = metadataOutbox
        if (this.selectedDatabaseId) {
          await this.loadDataOutbox()
        }
        this.lastUpdated = new Date()
      } catch (error) {
        this.showError(error)
      } finally {
        this.loading = false
        this.loadingDatabases = false
        this.loadingOutboxes = false
      }
    },
    async loadDataOutbox () {
      if (!this.selectedDatabaseId) {
        return
      }
      this.loadingOutboxes = true
      try {
        this.dataOutbox = await useReplicationService().findDataOutbox(this.selectedDatabaseId)
      } finally {
        this.loadingOutboxes = false
      }
    },
    async synchroniseDatabase () {
      this.synchronisingDatabase = true
      try {
        const result = await useReplicationService().synchroniseDatabase(this.selectedDatabaseId, this.pageSize)
        this.synchronisationResult = this.$t('replication.synchronisation.databaseResult', result)
        await this.refreshAll()
      } catch (error) {
        this.showError(error)
      } finally {
        this.synchronisingDatabase = false
      }
    },
    async synchroniseTable () {
      this.synchronisingTable = true
      try {
        const result = await useReplicationService().synchroniseTable(
          this.selectedDatabaseId,
          this.selectedTableId,
          this.pageSize
        )
        this.synchronisationResult = this.$t('replication.synchronisation.tableResult', result)
        await this.refreshAll()
      } catch (error) {
        this.showError(error)
      } finally {
        this.synchronisingTable = false
      }
    },
    async retryAll () {
      this.retryingAll = true
      const service = useReplicationService()
      try {
        if (this.activeOutbox === 'replication') {
          await service.retryReplicationOutbox()
        } else if (this.activeOutbox === 'metadata') {
          await service.retryMetadataOutbox()
        } else {
          await service.retryDataOutbox(this.selectedDatabaseId)
        }
        useToastInstance().success(this.$t('replication.retryQueued'))
        await this.refreshAll()
      } catch (error) {
        this.showError(error)
      } finally {
        this.retryingAll = false
      }
    },
    async retryEntry (source, id) {
      const service = useReplicationService()
      try {
        if (source === 'replication') {
          await service.retryReplicationOutboxEntry(id)
        } else if (source === 'metadata') {
          await service.retryMetadataOutboxEntry(id)
        } else {
          await service.retryDataOutboxEntry(this.selectedDatabaseId, id)
        }
        useToastInstance().success(this.$t('replication.retryQueued'))
        await this.refreshAll()
      } catch (error) {
        this.showError(error)
      }
    }
  }
}
</script>

<style scoped>
.replication-page {
  max-width: 1440px;
  margin: 0 auto;
  padding-bottom: 48px;
}

:deep(.v-data-table) {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

:deep(.v-data-table td) {
  max-width: 320px;
  overflow-wrap: anywhere;
}
</style>
