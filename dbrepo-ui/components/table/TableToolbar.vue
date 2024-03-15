<template>
  <div>
    <v-toolbar
      v-if="table"
      flat>
      <v-btn
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/table`" />
      <v-toolbar-title v-if="$vuetify.display.lgAndUp" v-text="table.name" />
      <v-spacer />
      <v-btn
        v-if="canImportCsv"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-cloud-upload' : null"
        color="tertiary"
        variant="flat"
        :text="$t('toolbars.database.import-csv.permanent') + ($vuetify.display.xlAndUp ? ' ' + $t('toolbars.database.import-csv.xl') : '')"
        class="ml-2"
        :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/import`" />
      <v-btn
        v-if="canExecuteQuery"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-wrench' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? $t('toolbars.database.create-subset.xl') + ' ' : '') + $t('toolbars.database.create-subset.permanent')"
        class="ml-2"
        :to="`/database/${$route.params.database_id}/subset/create?tid=${$route.params.table_id}`" />
      <v-btn
        v-if="canCreateView"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-view-carousel' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? $t('toolbars.database.create-view.xl') + ' ' : '') + $t('toolbars.database.create-view.permanent')"
        class="ml-2"
        :to="`/database/${$route.params.database_id}/view/create?tid=${$route.params.table_id}`" />
      <v-btn
        v-if="canDropTable"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-delete' : null"
        color="error"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? 'Drop ' : '') + 'Table'"
        class="ml-2"
        @click="dropTableDialog = true" />
      <v-btn
        v-if="canGetPid"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-content-save-outline' : null"
        color="primary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? 'Get ' : '') + 'PID'"
        class="ml-2"
        :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/persist`" />
      <template v-slot:extension>
        <v-tabs v-model="tab" color="primary">
          <v-tab
            :text="$t('navigation.info')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/info`" />
          <v-tab
            v-if="canViewTableData"
            :text="$t('navigation.data')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/data`" />
          <v-tab
            :text="$t('navigation.schema')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/schema`" />
        </v-tabs>
      </template>
    </v-toolbar>
    <v-dialog
      v-model="dropTableDialog"
      max-width="640">
      <DropTable @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import EditTuple from '@/components/dialogs/EditTuple'
import DropTable from '@/components/dialogs/DropTable'
import { useCacheStore } from '@/stores/cache'
import { useUserStore } from '@/stores/user'

export default {
  components: {
    EditTuple,
    DropTable
  },
  data () {
    return {
      tab: null,
      loading: false,
      error: false,
      edit: false,
      dropTableDialog: false,
      cacheStore: useCacheStore(),
      userStore: useUserStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    table () {
      return this.cacheStore.getTable
    },
    access () {
      return this.userStore.getAccess
    },
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    canExecuteQuery () {
      if (!this.roles) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access) && this.roles.includes('execute-query')
    },
    canDropTable () {
      if (!this.roles || !this.table || !this.user) {
        return false
      }
      if (this.roles.includes('delete-foreign-table')) {
        return true
      }
      const tableService = useTableService()
      return tableService.isOwner(this.table, this.user) && this.roles.includes('delete-table') && this.table.identifiers.length === 0
    },
    canCreateView () {
      if (!this.user) {
        return false
      }
      const databaseService = useDatabaseService()
      return databaseService.isOwner(this.database, this.user) && this.roles.includes('create-database-view')
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
    canGetPid () {
      if (!this.user || !this.table || !this.database) {
        return false
      }
      return this.database.owner.id === this.user.id || this.table.owner.id === this.user.id
    }
  },
  methods: {
    closed (event) {
      const { success } = event
      this.dropTableDialog = false
      if (success) {
        this.cacheStore.reloadDatabase()
        this.$router.push(`/database/${this.$route.params.database_id}/table`)
      }
    }
  }
}
</script>
