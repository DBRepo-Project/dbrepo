<template>
  <div>
    <TableToolbar />
    <v-toolbar
      v-if="canViewTableData"
      :color="versionColor"
      :title="title"
      flat>
      <v-spacer />
      <v-btn
        v-if="canAddTuple"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-plus' : null"
        variant="flat"
        :text="$t('toolbars.table.data.add')"
        class="ml-2"
        @click="addTuple" />
      <v-btn
        v-if="canEditTuple"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-pencil' : null"
        color="warning"
        variant="flat"
        :text="$t('toolbars.table.data.edit')"
        class="ml-2"
        @click="editTuple" />
      <v-btn
        v-if="canDeleteTuple"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-delete' : null"
        color="error"
        variant="flat"
        :text="$t('toolbars.table.data.delete')"
        class="ml-2"
        :loading="loadingDelete"
        @click="deleteItems" />
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-download' : null"
        variant="flat"
        :loading="downloadLoading"
        :text="$t('toolbars.table.data.download')"
        class="ml-2"
        @click.stop="download" />
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-refresh' : null"
        variant="flat"
        :text="$t('toolbars.table.data.refresh')"
        class="ml-2"
        :disabled="loadingData"
        :loading="loadingData"
        @click="reload" />
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-update' : null"
        variant="flat"
        :text="$t('toolbars.table.data.version')"
        class="ml-2"
        @click.stop="pick" />
    </v-toolbar>
    <TimeDrift />
    <v-card
      elevation="0"
      tile>
      <v-card
        v-if="error"
        variant="flat">
        <v-card-text>
          {{ $t('error.table.connection') }}
        </v-card-text>
      </v-card>
      <v-data-table-server
        v-if="!error"
        v-model="selection"
        flat
        :show-select="canModify"
        return-object
        :headers="headers"
        :items="rows"
        :items-length="total"
        :loading="loadingData || loadingCount"
        :options.sync="options"
        :footer-props="footerProps"
        :items-per-page-options="footerProps.itemsPerPageOptions"
        @update:options="loadData">
        <template
          v-for="(blobColumn, idx) in blobColumns"
          v-slot:[blobColumn]="{ item }">
          <BlobDownload
            :blob="item[blobColumn.substring(5)]" />
        </template>
      </v-data-table-server>
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
</template>

<script>
import TableHistory from '@/components/table/TableHistory.vue'
import TimeDrift from '@/components/TimeDrift.vue'
import TableToolbar from '@/components/table/TableToolbar.vue'
import {formatTimestampUTC, formatDateUTC, formatTimestamp} from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import EditTuple from '@/components/dialogs/EditTuple.vue'
import BlobDownload from '@/components/table/BlobDownload.vue'

export default {
  components: {
    BlobDownload,
    EditTuple,
    TableHistory,
    TableToolbar,
    TimeDrift
  },
  data () {
    return {
      loading: true,
      loadingData: false,
      loadingCount: false,
      loadingDelete: false,
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
      pickVersionDialog: null,
      version: null,
      lastReload: new Date(),
      tab: null,
      error: false,
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
    tables () {
      return this.cacheStore.getTable
    },
    access () {
      return this.userStore.getAccess
    },
    title () {
      return (this.version ? this.$t('toolbars.database.history') : this.$t('toolbars.database.current')) + ' ' + this.versionFormatted
    },
    blobColumns () {
      if (!this.table || !this.table.columns) {
        return []
      }
      return this.table.columns.filter(c => this.isFileField(c)).map(c => 'item.' + c.internal_name)
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
    canViewTableData () {
      /* view when database is public or when private: 1) view-table-data role present 2) access is at least read */
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (!this.roles || !this.roles.includes('view-table-data') || !this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_own' || this.access.type === 'write_all'
    },
    canAddTuple () {
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
      this.loadCount()
      this.reload()
    },
    table (newTable, oldTable) {
      if (newTable !== oldTable && oldTable === null) {
        this.loadProperties()
      }
    }
  },
  mounted () {
    this.loadProperties()
    this.loadCount()
  },
  methods: {
    addTuple () {
      this.tuple = {}
      this.table.columns.forEach((c) => {
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
        this.table.columns
          .filter(c => c.is_primary_key)
          .forEach((c) => {
            constraints[c.internal_name] = select[c.internal_name]
          })
        if (Object.keys(constraints).length === 0) {
          console.warn(`Table with id ${this.$route.params.table_id} does not have primary key(s): attempt to delete by values`)
          this.table.columns
            .forEach((c) => {
              constraints[c.internal_name] = select[c.internal_name]
            })
        }
        const tupleService = useTupleService()
        wait.push(tupleService.remove(this.$route.params.database_id, this.$route.params.table_id, { keys: constraints })
          .catch(({message}) => {
            const toast = useToastInstance()
            toast.error(message)
          }))
      }
      Promise.all(wait)
        .then(() => {
          const toast = useToastInstance()
          toast.success(`Deleted ${this.selection.length} row(s)`)
          this.$emit('modified', { success: true, action: 'delete' })
          this.selection = []
          this.reload()
        })
      this.loadingDelete = false
    },
    download () {
      this.downloadLoading = true
      if (!this.version) {
        const tableService = useTableService()
        tableService.exportData(this.$route.params.database_id, this.$route.params.table_id)
          .then((data) => {
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
        const tableService = useTableService()
        tableService.exportData(this.$route.params.database_id, this.$route.params.table_id, this.versionISO)
          .then((data) => {
            const url = URL.createObjectURL(data)
            const link = document.createElement('a')
            link.href = url
            link.download = `table_${this.versionISO}.csv`
            document.body.appendChild(link)
            link.click()
          })
          .catch(() => {
            this.downloadLoading = false
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
    loadProperties () {
      if (!this.table || this.headers.length > 0) {
        return
      }
      try {
        this.headers = []
        this.table.columns.map((c) => {
          return {
            value: c.internal_name,
            title: c.internal_name,
            sortable: false
          }
        }).forEach(header => this.headers.push(header))
        this.dateColumns = this.table.columns.filter(c => (c.column_type === 'date' || c.column_type === 'timestamp'))
        console.debug('date columns are', this.dateColumns)
      } catch ({code}) {
        const toast = useToastInstance()
        if (typeof code !== 'string') {
          return
        }
        toast.error(this.$t(code))
      }
      this.loading = false
    },
    reload () {
      this.lastReload = new Date()
      this.loadData({ page: this.options.page, itemsPerPage: this.options.itemsPerPage, sortBy: null})
    },
    loadCount() {
      this.loadingCount = true
      const tableService = useTableService()
      tableService.getCount(this.$route.params.database_id, this.$route.params.table_id, (this.versionISO || this.lastReload.toISOString()))
        .then((count) => {
          this.total = count
          this.loadingCount = false
        })
        .catch((error) => {
          this.loadingCount = false
        })
    },
    loadData({ page, itemsPerPage, sortBy }) {
      this.options.page = page
      this.options.itemsPerPage = itemsPerPage
      const tableService = useTableService()
      this.loadingData = true
      tableService.getData(this.$route.params.database_id, this.$route.params.table_id, (page - 1), itemsPerPage, (this.versionISO || this.lastReload.toISOString()))
        .then((data) => {
          this.rows = data.result.map((row) => {
            for (const col in row) {
              const column = this.table.columns.filter(c => c.internal_name === col)[0]
              const columnDefinition = this.dateColumns.filter(c => c.internal_name === col)
              if (columnDefinition.length > 0) {
                if (columnDefinition[0].column_type === 'date') {
                  row[col] = formatDateUTC(row[col])
                } else if (columnDefinition[0].column_type === 'timestamp') {
                  row[col] = formatTimestampUTC(row[col])
                }
              }
            }
            return row
          })
          this.loadingData = false
        })
        .catch(({code, message}) => {
          this.error = true
          this.loadingData = false
          const toast = useToastInstance()
          if (typeof code !== 'string' || typeof message !== 'string') {
            return
          }
          toast.error(this.$t(code) + ": " + message)
        })
    },
    isFileField (column) {
      return ['blob', 'longblob', 'mediumblob', 'tinyblob'].includes(column.column_type)
    },
    close ({ success }) {
      console.debug('closed edit/create tuple dialog')
      this.addTupleDialog = false
      this.editTupleDialog = false
      if (success) {
        this.reload()
        this.selection = []
      }
    }
  }
}
</script>

