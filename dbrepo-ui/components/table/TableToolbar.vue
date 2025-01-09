<template>
  <div>
    <v-toolbar
      flat>
      <v-btn
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/table`" />
      <v-toolbar-title
        v-if="table">
        <v-skeleton-loader
          v-if="!table && $vuetify.display.lgAndUp"
          type="subtitle"
          width="200" />
        <span
          v-if="table && $vuetify.display.lgAndUp">
          {{ table.name }}
        </span>
        <ResourceStatus
          class="ml-2"
          :resource="table" />
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        v-if="canImportCsv"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-cloud-upload' : null"
        color="tertiary"
        :variant="buttonVariant"
        :text="$t('toolbars.database.import-csv.permanent') + ($vuetify.display.lgAndUp ? ' ' + $t('toolbars.database.import-csv.xl') : '')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/import`" />
      <v-btn
        v-if="canExecuteQuery"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-wrench' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.create-subset.xl') + ' ' : '') + $t('toolbars.database.create-subset.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/subset/create?tid=${$route.params.table_id}`" />
      <v-btn
        v-if="canCreateView"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-view-carousel' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.create-view.xl') + ' ' : '') + $t('toolbars.database.create-view.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/view/create?tid=${$route.params.table_id}`" />
      <v-btn
        v-if="canUpdateTable"
        class="mr-2"
        variant="flat"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-table-edit' : null"
        color="warning"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.update-table.xl') + ' ' : '') + $t('toolbars.database.update-table.permanent')"
        @click="updateTableDialog = true" />
      <v-btn
        v-if="canDropTable"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-delete' : null"
        color="error"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? 'Drop ' : '') + 'Table'"
        class="mr-2"
        @click="dropTableDialog = true" />
      <v-btn
        v-if="canGetPid"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-content-save-outline' : null"
        color="primary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? 'Get ' : '') + 'PID'"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/persist`" />
      <template v-slot:extension>
        <v-tabs v-model="tab" color="primary">
          <v-tab
            :text="$t('navigation.info')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/info`" />
          <v-tab
            v-if="canViewData"
            :text="$t('navigation.data')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/data`" />
          <v-tab
            v-if="canViewSchema"
            :text="$t('navigation.schema')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/schema`" />
        </v-tabs>
      </template>
    </v-toolbar>
    <v-dialog
      v-model="dropTableDialog"
      max-width="640">
      <DropTable
        @close="closeDelete" />
    </v-dialog>
    <v-dialog
      v-model="updateTableDialog"
      max-width="640">
      <UpdateTable
        :table="table"
        @close="closeUpdate" />
    </v-dialog>
  </div>
</template>

<script>
import EditTuple from '@/components/dialogs/EditTuple.vue'
import DropTable from '@/components/dialogs/DropTable.vue'
import UpdateTable from '@/components/dialogs/UpdateTable.vue'
import { useCacheStore } from '@/stores/cache'
import { useUserStore } from '@/stores/user'

export default {
  components: {
    EditTuple,
    DropTable,
    UpdateTable
  },
  data () {
    return {
      tab: null,
      loading: false,
      error: false,
      edit: false,
      dropTableDialog: false,
      updateTableDialog: false,
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
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    canUpdateTable () {
      if (!this.roles || !this.user || !this.table) {
        return false
      }
      return this.roles.includes('update-table') && this.table.owner.id === this.user.id
    },
    canExecuteQuery () {
      if (!this.roles || !this.table || !this.user) {
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
      if (!this.roles || !this.table || !this.user) {
        return false
      }
      const databaseService = useDatabaseService()
      return databaseService.isOwner(this.database, this.user) && this.roles.includes('create-database-view')
    },
    canViewData () {
      if (!this.table) {
        return false
      }
      if (this.table.is_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.hasReadAccess || this.table.owned_by === this.user.id || this.database.owner.id === this.user.id
    },
    canViewSchema () {
      if (!this.table) {
        return false
      }
      if (this.table.is_schema_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.hasReadAccess || this.table.owned_by === this.user.id || this.database.owner.id === this.user.id
    },
    canImportCsv () {
      if (!this.roles || !this.table || !this.user) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    canGetPid () {
      if (!this.user || !this.table || !this.database) {
        return false
      }
      return this.database.owner.id === this.user.id || this.table.owner.id === this.user.id
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    isContrastTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast')
    },
    isDarkTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().startsWith('dark')
    },
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
    },
  },
  methods: {
    closeDelete ({success}) {
      this.dropTableDialog = false
      if (success) {
        this.cacheStore.reloadDatabase()
        this.$router.push(`/database/${this.$route.params.database_id}/table`)
      }
    },
    closeUpdate () {
      this.updateTableDialog = false
    }
  }
}
</script>
