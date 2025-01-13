<template>
  <div
    v-if="canViewTableData">
    <TableToolbar />
    <v-toolbar
      :color="versionColor"
      :title="title"
      flat>
      <v-spacer />
      <v-btn
        v-if="canAddTuple"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-plus' : null"
        variant="flat"
        :text="$t('toolbars.table.data.add')"
        class="ml-2"
        @click="addTuple" />
      <v-btn
        v-if="canEditTuple"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-pencil' : null"
        color="warning"
        variant="flat"
        :text="$t('toolbars.table.data.edit')"
        class="ml-2"
        @click="editTuple" />
      <v-btn
        v-if="canDeleteTuple"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-delete' : null"
        color="error"
        variant="flat"
        :text="$t('toolbars.table.data.delete')"
        class="ml-2"
        :loading="loadingDelete"
        @click="deleteItems" />
      <v-btn
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-download' : null"
        variant="flat"
        :loading="downloadLoading"
        :text="$t('toolbars.table.data.download')"
        class="ml-2"
        @click.stop="download" />
      <v-btn
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-refresh' : null"
        variant="flat"
        :text="$t('toolbars.table.data.refresh')"
        class="ml-2"
        :disabled="loadingData"
        :loading="loadingData"
        @click="reload" />
      <v-btn
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-update' : null"
        variant="flat"
        :text="$t('toolbars.table.data.version')"
        class="ml-2 mr-2"
        @click.stop="pick" />
    </v-toolbar>
    <TimeDrift />
    <v-card
      v-if="error"
      variant="flat">
      <v-card-text>
        {{ $t('error.table.connection') }}
      </v-card-text>
    </v-card>
    <v-card
      tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
        class="mt-0 mb-0"
        type="table"
        :select="canSelectTuples"
        :timestamp="versionISO || lastReload.toISOString()"
        @selection="updateSelect" />
    </v-card>
    <v-dialog
      v-model="pickVersionDialog"
      max-width="640"
      @close="closeVersion">
      <TableHistory
        ref="timeTravel"
        @close="pickVersion" />
    </v-dialog>
    <v-dialog
      v-model="addTupleDialog"
      persistent
      max-width="640">
      <EditTuple
        :table="table"
        :tuple="tuple"
        :edit="false"
        @close="close" />
    </v-dialog>
    <v-dialog
      v-model="editTupleDialog"
      persistent
      max-width="640">
      <EditTuple
        :table="table"
        :tuple="tuple"
        :edit="true"
        @close="close" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'table' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'table' })" />
</template>

<script>
import TableHistory from '@/components/table/TableHistory.vue'
import TimeDrift from '@/components/TimeDrift.vue'
import TableToolbar from '@/components/table/TableToolbar.vue'
import { errorCodeKey, formatTimestamp } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import EditTuple from '@/components/dialogs/EditTuple.vue'
import BlobDownload from '@/components/table/BlobDownload.vue'
import QueryResults from '@/components/subset/Results.vue'
import JumboBox from '@/components/JumboBox.vue'

export default {
  components: {
    QueryResults,
    BlobDownload,
    EditTuple,
    TableHistory,
    TableToolbar,
    TimeDrift,
    JumboBox
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
      error
    }
  },
  data () {
    return {
      loading: true,
      loadingData: false,
      loadingCount: false,
      loadingDelete: false,
      loadingTable: false,
      addTupleDialog: false,
      editTupleDialog: false,
      total: 0,
      footerProps: {
        showFirstLastPage: true,
        itemsPerPageOptions: [10, 25, 50, 100]
      },
      downloadLoading: false,
      dateMenu: false,
      timeMenu: false,
      selection: [],
      columns: [],
      pickVersionDialog: null,
      version: null,
      lastReload: new Date(),
      tab: null,
      tuple: null,
      options: {
        page: 1,
        itemsPerPage: 10
      },
      dateColumns: [],
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
          title: this.$t('navigation.data'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data`,
          disabled: true
        }
      ],
      headers: [],
      rows: [],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    roles () {
      return this.userStore.getRoles
    },
    database () {
      return this.cacheStore.getDatabase
    },
    table () {
      return this.cacheStore.getTable
    },
    user () {
      return this.userStore.getUser
    },
    access () {
      return this.userStore.getAccess
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    title () {
      return (this.version ? this.$t('toolbars.database.history') : this.$t('toolbars.database.current')) + ' ' + this.versionFormatted
    },
    blobColumns () {
      if (!this.columns) {
        return []
      }
      return this.columns.filter(c => this.isFileField(c)).map(c => 'item.' + c.internal_name)
    },
    versionColor () {
      return this.version ? 'primary' : 'secondary'
    },
    versionFormatted () {
      if (this.version === null) {
        return ''
      }
      return this.version + ' (UTC)'
    },
    versionISO () {
      if (this.version === null) {
        return null
      }
      return this.version.substring(0, 10) + 'T' + this.version.substring(11, 19) + 'Z'
    },
    canModify () {
      if (!this.user || !this.access || !this.table) {
        return false
      }
      if (this.access.type === 'write_own' && this.table.owner.id === this.user.id) {
        return true
      }
      return this.access.type === 'write_all'
    },
    primaryKeyColumns () {
      if (!this.table) {
        return []
      }
      return this.table.constraints.primary_key.map(pk => pk.column)
    },
    canViewTableData () {
      if (this.error) {
        return false
      }
      if (!this.table) {
        return false
      }
      if (this.table.is_public) {
        return true
      }
      if (!this.roles || !this.roles.includes('view-table-data')) {
        return false
      }
      return this.hasReadAccess
    },
    canAddTuple () {
      if (!this.roles) {
        return false
      }
      const userService = useUserService()
      return userService.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('insert-table-data')
    },
    canSelectTuples () {
      if (!this.roles) {
        return false
      }
      const userService = useUserService()
      return userService.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('insert-table-data')
    },
    canEditTuple () {
      if (!this.roles || this.selection === null || this.selection.length !== 1) {
        return false
      }
      const userService = useUserService()
      return userService.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('insert-table-data')
    },
    canDeleteTuple () {
      if (!this.roles || this.selection === null || this.selection.length < 1) {
        return false
      }
      const userService = useUserService()
      return userService.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('delete-table-data')
    }
  },
  watch: {
    version () {
      this.reload()
    }
  },
  mounted () {
    this.reload()
  },
  methods: {
    errorCodeKey,
    addTuple () {
      this.tuple = {}
      this.columns.forEach((c) => {
        this.tuple[c.internal_name] = null
      })
      this.addTupleDialog = true
    },
    editTuple () {
      this.tuple = this.selection[0]
      this.editTupleDialog = true
    },
    deleteItems () {
      this.loadingDelete = true
      const wait = []
      for (const select of this.selection) {
        /* remove in container */
        const constraints = {}
        this.primaryKeyColumns
          .forEach((c) => {
            constraints[c.internal_name] = select[c.internal_name]
          })
        if (Object.keys(constraints).length === 0) {
          console.warn(`Table with id ${this.$route.params.table_id} does not have primary key(s): attempt to delete by values`)
          this.columns
            .forEach((c) => {
              constraints[c.internal_name] = select[c.internal_name]
            })
        }
        const tupleService = useTupleService()
        wait.push(tupleService.remove(this.$route.params.database_id, this.$route.params.table_id, { keys: constraints })
          .catch(({code, message}) => {
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              return
            }
            toast.error(this.$t(code))
          }))
      }
      Promise.all(wait)
        .then(() => {
          const toast = useToastInstance()
          toast.success(`Deleted ${this.selection.length} row(s)`)
          this.$emit('modified', { success: true, action: 'delete' })
          this.selection = []
          this.$refs.queryResults.resetSelection()
          this.reload()
        })
      this.loadingDelete = false
    },
    download () {
      this.downloadLoading = true
      const tableService = useTableService()
      if (!this.version) {
        tableService.exportData(this.$route.params.database_id, this.$route.params.table_id)
          .then((data) => {
            this.downloadLoading = false
            const url = URL.createObjectURL(data)
            const link = document.createElement('a')
            link.href = url
            link.download = 'table.csv'
            document.body.appendChild(link)
            link.click()
          })
          .catch(({code}) => {
            this.downloadLoading = false
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.downloadLoading = false
          })
      } else {
        tableService.exportData(this.$route.params.database_id, this.$route.params.table_id, this.versionISO)
          .then((data) => {
            this.downloadLoading = false
            const url = URL.createObjectURL(data)
            const link = document.createElement('a')
            link.href = url
            link.download = `table_${this.versionISO}.csv`
            document.body.appendChild(link)
            link.click()
          })
          .catch(({code}) => {
            this.downloadLoading = false
            const toast = useToastInstance()
            if (typeof code !== 'string') {
              return
            }
            toast.error(this.$t(code))
          })
          .finally(() => {
            this.downloadLoading = false
          })
      }
    },
    pick () {
      this.pickVersionDialog = true
    },
    closeVersion () {
      this.pickVersionDialog = false
    },
    pickVersion (event) {
      const { success, timestamp } = event
      if (success) {
        if (timestamp === null) {
          this.version = null
        } else {
          this.version = formatTimestamp(timestamp)
        }
      }
      this.pickVersionDialog = false
    },
    reload () {
      this.lastReload = new Date()
      if (!this.canViewTableData) {
        return
      }
      this.$refs.queryResults.reExecute(Number(this.$route.params.table_id))
      this.$refs.queryResults.reExecuteCount(Number(this.$route.params.table_id))
    },
    isFileField (column) {
      return ['blob', 'longblob', 'mediumblob', 'tinyblob'].includes(column.type)
    },
    close ({ success }) {
      console.debug('closed edit/create tuple dialog')
      this.addTupleDialog = false
      this.editTupleDialog = false
      if (success) {
        this.reload()
        this.selection = []
        this.$refs.queryResults.resetSelection()
      }
    },
    updateSelect (selection) {
      this.selection = selection
    }
  }
}
</script>

