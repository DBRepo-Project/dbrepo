<template>
  <div>
    <v-toolbar
      flat>
      <v-toolbar-title>
        <v-skeleton-loader
          v-if="!database"
          type="subtitle"
          width="200" />
        <span
          class="mr-2"
          v-if="database && $vuetify.display.mdAndUp">
          {{ database.name }}
        </span>
        <ResourceStatus
          :size="$vuetify.display.mdAndUp ? 'small' : 'default'"
          :resource="database" />
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        v-if="false"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-chart-timeline-variant-shimmer' : null"
        color="tertiary"
        :variant="buttonVariant"
        :text="$t('toolbars.database.dashboard.permanent') + ($vuetify.display.mdAndUp ? ' ' + $t('toolbars.database.dashboard.xl') : '')" />
      <v-btn
        v-if="canCreateTable"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-table-large-plus' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? $t('toolbars.database.create-table.xl') + ' ' : '') + $t('toolbars.database.create-table.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/table/create/dataset`" />
      <v-btn
        v-if="canCreateSubset"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-wrench' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? $t('toolbars.database.create-subset.xl') + ' ' : '') + $t('toolbars.database.create-subset.permanent')"
        class="mr-2 white--text"
        :to="`/database/${$route.params.database_id}/subset/create`" />
      <v-btn
        v-if="canCreateView"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-view-carousel-outline' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? $t('toolbars.database.create-view.xl') + ' ' : '') + $t('toolbars.database.create-view.permanent')"
        class="mr-2 white--text"
        :to="`/database/${$route.params.database_id}/view/create`" />
      <v-btn
        v-if="canCreateIdentifier"
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-identifier' : null"
        color="primary"
        variant="flat"
        :text="($vuetify.display.mdAndUp ? $t('toolbars.database.create-pid.xl') + ' ' : '') + $t('toolbars.database.create-pid.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/persist`" />
      <template v-slot:extension>
        <v-tabs
          v-model="tab"
          color="primary">
          <v-tab
            :text="$t('toolbars.database.info.tab')"
            :to="`/database/${$route.params.database_id}/info`" />
          <v-tab
            :text="$t('toolbars.database.views.tab')"
            :to="`/database/${$route.params.database_id}/view`" />
          <v-tab
            v-if="canViewSubsets"
            :text="$t('toolbars.database.subsets.tab')"
            :to="`/database/${$route.params.database_id}/subset`" />
          <v-tab
            :text="$t('toolbars.database.tables.tab')"
            :to="`/database/${$route.params.database_id}/table`" />
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
import { useCacheStore } from '@/stores/cache.js'
import ResourceStatus from '@/components/ResourceStatus.vue'

export default {
  components: {
    ResourceStatus
  },
  data () {
    return {
      tab: null,
      error: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.cacheStore.getAccess
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
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
    canCreateIdentifier () {
      if (!this.roles || !this.access) {
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
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canCreateSubset () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      return this.hasReadAccess
    },
    canCreateView () {
      if (!this.cacheUser || !this.isOwner || !this.roles || !this.access) {
        return false
      }
      return this.roles.includes('create-database-view')
    },
    canCreateTable () {
      if (!this.cacheUser || !this.hasWriteAccess || !this.roles) {
        return false
      }
      return this.roles.includes('create-table')
    },
    canViewSubsets () {
      return this.hasReadAccess
    },
    isOwner () {
      if (!this.database || !this.cacheUser) {
        return false
      }
      return this.database.owner.id === this.cacheUser.uid
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  }
}
</script>
