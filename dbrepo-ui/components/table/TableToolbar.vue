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
          v-if="!table && $vuetify.display.mdAndUp"
          type="subtitle"
          width="200" />
        <span
          class="mr-2"
          v-if="table && $vuetify.display.mdAndUp">
          {{ table.name }}
        </span>
        <ResourceStatus
          :size="$vuetify.display.mdAndUp ? 'small' : 'default'"
          :resource="table" />
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        v-if="canImportCsv"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-cloud-upload' : null"
        color="tertiary"
        :variant="buttonVariant"
        :text="$t('toolbars.database.import-csv.permanent') + ($vuetify.display.mdAndUp ? ' ' + $t('toolbars.database.import-csv.xl') : '')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/import`" />
      <v-btn
        v-if="canExecuteQuery"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-wrench' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? $t('toolbars.database.create-subset.xl') + ' ' : '') + $t('toolbars.database.create-subset.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/subset/create?tid=${$route.params.table_id}`" />
      <v-btn
        v-if="canCreateView"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-view-carousel' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? $t('toolbars.database.create-view.xl') + ' ' : '') + $t('toolbars.database.create-view.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/view/create?tid=${$route.params.table_id}`" />
      <v-btn
        v-if="canGetPid"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-identifier' : null"
        color="primary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? 'Get ' : '') + 'PID'"
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
          <v-tab
            v-if="canUpdateTable"
            :text="$t('navigation.settings')"
            :to="`/database/${$route.params.database_id}/table/${$route.params.table_id}/settings`" />
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
import EditTuple from '@/components/dialogs/EditTuple.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    EditTuple
  },
  data () {
    return {
      tab: null,
      loading: false,
      error: false,
      edit: false,
      dropTableDialog: false,
      cacheStore: useCacheStore()
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
      return this.cacheStore.getAccess
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    roles () {
      return this.cacheStore.getRoles
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    canUpdateTable () {
      if (!this.roles || !this.cacheUser || !this.table) {
        return false
      }
      return this.roles.includes('update-table') && this.table.owner.id === this.cacheUser.uid
    },
    canExecuteQuery () {
      if (!this.roles || !this.table || !this.cacheUser) {
        return false
      }
      return this.hasReadAccess && this.roles.includes('execute-query')
    },
    isOwner () {
      const databaseService = useDatabaseService()
      return databaseService.isOwner(this.database, this.cacheUser)
    },
    canCreateView () {
      if (!this.roles || !this.table || !this.cacheUser) {
        return false
      }
      return this.isOwner && this.roles.includes('create-database-view')
    },
    canViewData () {
      if (!this.table) {
        return false
      }
      if (this.table.is_public) {
        return true
      }
      if (!this.cacheUser) {
        return false
      }
      return this.hasReadAccess || this.table.owner.id === this.cacheUser.uid || this.database.owner.id === this.cacheUser.uid
    },
    canViewSchema () {
      if (!this.table) {
        return false
      }
      if (this.table.is_schema_public) {
        return true
      }
      if (!this.cacheUser) {
        return false
      }
      return this.hasReadAccess || this.table.owner.id === this.cacheUser.uid || this.database.owner.id === this.cacheUser.uid
    },
    canImportCsv () {
      if (!this.roles || !this.table || !this.cacheUser) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    canGetPid () {
      if (!this.cacheUser || !this.table || !this.database) {
        return false
      }
      return this.hasReadAccess && this.database.owner.id === this.cacheUser.uid || this.table.owner.id === this.cacheUser.uid
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  }
}
</script>
