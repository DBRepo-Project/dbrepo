<template>
  <div>
    <v-toolbar
      flat>
      <v-toolbar-title>
        <span
          v-if="database && $vuetify.display.lgAndUp"
          v-text="database.name" />
        <v-tooltip
          v-if="database"
          bottom>
          <template v-slot:activator="{ props }">
            <v-icon
              class="ml-2"
              size="small"
              right
              :color="database.is_public ? 'success' : 'chip'"
              v-bind="props">
              {{ database.is_public ? 'mdi-lock-open-outline' : 'mdi-lock-outline' }}
            </v-icon>
          </template>
          <span>{{ $t('toolbars.database.' + (database.is_public ? 'public' : 'private')) }}</span>
        </v-tooltip>
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        v-if="canImportCsv"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-cloud-upload' : null"
        color="tertiary"
        :variant="buttonVariant"
        :text="$t('toolbars.database.import-csv.permanent') + ($vuetify.display.xlAndUp ? ' ' + $t('toolbars.database.import-csv.xl') : '')"
        :to="`/database/${$route.params.database_id}/table/import`" />
      <v-btn
        v-if="canCreateSubset"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-wrench' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? $t('toolbars.database.create-subset.xl') + ' ' : '') + $t('toolbars.database.create-subset.permanent')"
        class="ml-2 white--text"
        :to="`/database/${$route.params.database_id}/subset/create`" />
      <v-btn
        v-if="canCreateView"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-view-carousel-outline' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? $t('toolbars.database.create-view.xl') + ' ' : '') + $t('toolbars.database.create-view.permanent')"
        class="ml-2 white--text"
        :to="`/database/${$route.params.database_id}/view/create`" />
      <v-btn
        v-if="canCreateTable"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-table-large-plus' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? $t('toolbars.database.create-table.xl') + ' ' : '') + $t('toolbars.database.create-table.permanent')"
        class="ml-2"
        :to="`/database/${$route.params.database_id}/table/create`" />
      <v-btn
        v-if="canCreateIdentifier"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-identifier' : null"
        color="primary"
        variant="flat"
        :text="($vuetify.display.xlAndUp ? $t('toolbars.database.create-pid.xl') + ' ' : '') + $t('toolbars.database.create-pid.permanent')"
        class="ml-2"
        :to="`/database/${$route.params.database_id}/persist`" />
      <template v-slot:extension>
        <v-tabs
          v-model="tab"
          color="primary">
          <v-tab
            :text="$t('toolbars.database.info.tab')"
            :to="`/database/${$route.params.database_id}/info`" />
          <v-tab
            :text="$t('toolbars.database.tables.tab')"
            :to="`/database/${$route.params.database_id}/table`" />
          <v-tab
            :text="$t('toolbars.database.subsets.tab')"
            :to="`/database/${$route.params.database_id}/subset`" />
          <v-tab
            :text="$t('toolbars.database.views.tab')"
            :to="`/database/${$route.params.database_id}/view`" />
          <v-tab
            v-if="isOwner"
            :text="$t('toolbars.database.settings.tab')"
            :to="`/database/${$route.params.database_id}/settings`" />
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'
import { useUserStore } from '@/stores/user'

export default {
  data () {
    return {
      tab: null,
      error: false,
      cacheStore: useCacheStore(),
      userStore: useUserStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
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
    canCreateIdentifier () {
      if (!this.roles) {
        return false
      }
      if (this.roles.includes('create-foreign-identifier')) {
        return true
      }
      return this.roles.includes('create-identifier') && this.isOwner
    },
    hasWriteAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canImportCsv () {
      if (!this.user || !this.hasWriteAccess) {
        return false
      }
      return this.roles.includes('insert-table-data')
    },
    canCreateSubset () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('execute-query')
    },
    canCreateView () {
      if (!this.user || !this.isOwner) {
        return false
      }
      return this.roles.includes('create-database-view')
    },
    canCreateTable () {
      if (!this.user || !this.hasWriteAccess) {
        return false
      }
      return this.roles.includes('create-table')
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      return this.database.owner.username === this.user.username
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  }
}
</script>
