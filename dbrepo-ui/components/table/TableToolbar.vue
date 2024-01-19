<template>
  <div v-if="table">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" plain :to="`/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        <span v-if="$vuetify.breakpoint.lgAndUp" v-text="table.name" />
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canAddTuple" class="mb-1" @click="addTuple">
          <v-icon :left="$vuetify.breakpoint.xlOnly">mdi-plus</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Add</span>
        </v-btn>
        <v-btn v-if="canEditTuple" color="warning" class="mb-1 black--text" @click="editTuple">
          <v-icon :left="$vuetify.breakpoint.xlOnly">mdi-pencil</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Edit</span>
        </v-btn>
        <v-btn v-if="canDeleteTuple" color="error" class="mb-1" :loading="loadingDelete" @click="deleteItems">
          <v-icon :left="$vuetify.breakpoint.xlOnly">mdi-delete</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Delete</span>
        </v-btn>
        <v-btn v-if="canImportCsv" class="mb-1" :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/import`">
          <v-icon left>mdi-cloud-upload</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Import&nbsp;</span> csv
        </v-btn>
        <v-btn v-if="canExecuteQuery" class="mb-1" :to="`/database/${$route.params.database_id}/query/create?tid=${$route.params.table_id}`" color="secondary">
          <v-icon left>mdi-wrench</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Create&nbsp;</span> Subset
        </v-btn>
        <v-btn v-if="canCreateView" class="mb-1" :to="`/database/${$route.params.database_id}/view/create?tid=${$route.params.table_id}`" color="secondary">
          <v-icon left>mdi-view-carousel</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Create&nbsp;</span> View
        </v-btn>
        <v-btn v-if="canDropTable" class="mb-1" color="error" @click="dropTableDialog = true">
          <v-icon left>mdi-delete</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Drop&nbsp;</span> Table
        </v-btn>
        <v-btn v-if="canGetPid" class="mb-1" color="primary" :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/persist`">
          <v-icon left>mdi-content-save-outline</v-icon> <span v-if="$vuetify.breakpoint.xlOnly">Get&nbsp;</span> PID
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-tabs v-model="tab" color="primary">
      <v-tab :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/info`">
        Info
      </v-tab>
      <v-tab v-if="canViewTableData" :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/data`">
        Data
      </v-tab>
      <v-tab :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/schema`">
        Schema
      </v-tab>
    </v-tabs>
    <v-dialog
      v-model="editTupleDialog"
      persistent
      max-width="640">
      <EditTuple :table="table" :tuple="tuple" :edit="edit" @close="close" />
    </v-dialog>
    <v-dialog
      v-model="dropTableDialog"
      max-width="640">
      <DropTable @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import EditTuple from '@/components/dialogs/EditTuple.vue'
import TableService from '@/api/table.service'
import UserUtils from '@/api/user.utils'
import DatabaseUtils from '@/api/database.utils'
import DropTable from '@/components/dialogs/DropTable.vue'
import TableUtils from '@/api/table.utils'

export default {
  components: {
    EditTuple,
    DropTable
  },
  props: {
    selection: {
      type: Array,
      default: () => {
        return []
      }
    }
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingDelete: false,
      error: false,
      edit: false,
      editTupleDialog: false,
      dropTableDialog: false
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
    },
    access () {
      return this.$store.state.access
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    canAddTuple () {
      if (!this.roles || !this.isDataTab) {
        return false
      }
      return UserUtils.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('insert-table-data')
    },
    canEditTuple () {
      if (this.selection === null || this.selection.length !== 1) {
        return false
      }
      if (!this.roles || !this.isDataTab) {
        return false
      }
      return UserUtils.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('insert-table-data')
    },
    canDeleteTuple () {
      if (this.selection === null || this.selection.length < 1) {
        return false
      }
      if (!this.roles || !this.isDataTab) {
        return false
      }
      return UserUtils.hasWriteAccess(this.table, this.access, this.user) && this.roles.includes('delete-table-data')
    },
    canExecuteQuery () {
      if (!this.roles) {
        return false
      }
      return UserUtils.hasReadAccess(this.access) && this.roles.includes('execute-query')
    },
    canDropTable () {
      if (!this.roles || !this.table || !this.user) {
        return false
      }
      if (this.roles.includes('delete-foreign-table')) {
        return true
      }
      return TableUtils.isOwner(this.table, this.user) && this.roles.includes('delete-table') && this.table.identifiers.length === 0
    },
    canCreateView () {
      if (!this.user) {
        return false
      }
      return DatabaseUtils.isOwner(this.database, this.user) && this.roles.includes('create-database-view')
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
    canImportCsv () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    isDataTab () {
      return String(this.tab).endsWith('data')
    },
    tuple () {
      return this.edit ? this.selection[0] : {}
    },
    canGetPid () {
      if (!this.user || !this.table || !this.database) {
        return false
      }
      return this.database.owner.id === this.user.id || this.table.owner.id === this.user.id
    }
  },
  methods: {
    addTuple () {
      const data = {}
      this.edit = false
      this.table.columns.forEach((c) => {
        data[c.internal_name] = null
      })
      this.selection = []
      this.editTupleDialog = true
    },
    editTuple () {
      this.edit = true
      this.editTupleDialog = true
    },
    pick () {
      if (this.$refs.timeTravel !== undefined) {
        /* when the component was loaded once, this method refreshes the content */
        this.$refs.timeTravel.loadHistory()
      }
      this.pickVersionDialog = true
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
        wait.push(TableService.deleteTuple(this.$route.params.database_id, this.$route.params.table_id, { keys: constraints }))
      }
      Promise.all(wait)
        .then(() => {
          this.$toast.success(`Deleted ${this.selection.length} row(s)`)
          this.$emit('modified', { success: true, action: 'delete' })
        })
      this.loadingDelete = false
    },
    close (event) {
      console.debug('closed edit/create tuple dialog', event)
      this.editTupleDialog = false
      if (event.success) {
        this.$emit('modified', { success: true, action: 'save' })
      } else {
        this.$emit('modified', { success: false, action: 'close' })
      }
    },
    async closed (event) {
      console.debug('closed drop table dialog', event)
      this.dropTableDialog = false
      await this.$store.dispatch('reloadDatabase')
      await this.$router.push(`/database/${this.$route.params.database_id}/table`)
    }
  }
}
</script>
