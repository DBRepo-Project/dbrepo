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
          v-if="database && $vuetify.display.lgAndUp">
          {{ database.name }}
        </span>
        <v-chip
          v-if="database && database.is_public"
          size="small"
          class="ml-2"
          color="success"
          :text="$t('toolbars.database.public')"
          variant="outlined" />
        <v-chip
          v-if="database && !database.is_public"
          size="small"
          class="ml-2"
          :color="colorVariant"
          variant="outlined"
          :text="$t('toolbars.database.private')"
          flat />
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        v-if="false"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-chart-timeline-variant-shimmer' : null"
        color="tertiary"
        :variant="buttonVariant"
        :text="$t('toolbars.database.dashboard.permanent') + ($vuetify.display.lgAndUp ? ' ' + $t('toolbars.database.dashboard.xl') : '')" />
      <v-btn
        v-if="canCreateTable"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-table-large-plus' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.create-table.xl') + ' ' : '') + $t('toolbars.database.create-table.permanent')"
        class="mr-2"
        :to="`/database/${$route.params.database_id}/table/create/dataset`" />
      <v-btn
        v-if="canCreateSubset"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-wrench' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.create-subset.xl') + ' ' : '') + $t('toolbars.database.create-subset.permanent')"
        class="mr-2 white--text"
        :to="`/database/${$route.params.database_id}/subset/create`" />
      <v-btn
        v-if="canCreateView"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-view-carousel-outline' : null"
        color="secondary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.create-view.xl') + ' ' : '') + $t('toolbars.database.create-view.permanent')"
        class="mr-2 white--text"
        :to="`/database/${$route.params.database_id}/view/create`" />
      <v-btn
        v-if="canCreateIdentifier"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-identifier' : null"
        color="primary"
        variant="flat"
        :text="($vuetify.display.lgAndUp ? $t('toolbars.database.create-pid.xl') + ' ' : '') + $t('toolbars.database.create-pid.permanent')"
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
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
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
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canImportCsv () {
      if (!this.user || !this.hasWriteAccess) {
        return false
      }
      return this.roles.includes('insert-table-data')
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
