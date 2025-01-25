<template>
  <v-toolbar flat>
    <v-btn
      size="small"
      icon="mdi-arrow-left"
      :to="`/database/${$route.params.database_id}/view`" />
    <v-toolbar-title
      v-if="view">
      <span
        v-if="$vuetify.display.mdAndUp"
        class="mr-2">
        {{ title }}
      </span>
      <ResourceStatus
        :size="$vuetify.display.mdAndUp ? 'small' : 'default'"
        :resource="view" />
    </v-toolbar-title>
    <v-spacer />
    <v-btn
      v-if="canCreatePid"
      class="mr-2"
      :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-content-save-outline' : null"
      variant="flat"
      color="primary"
      :text="($vuetify.display.mdAndUp ? $t('toolbars.view.pid.xl') + ' ' : '') + $t('toolbars.view.pid.permanent')"
      :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/persist`" />
    <v-dialog
      v-model="updateViewDialog"
      persistent
      max-width="640">
      <ViewVisibility
        :view="view"
        @close="close" />
    </v-dialog>
    <template v-slot:extension>
      <v-tabs
        v-model="tab"
        color="primary">
        <v-tab
          :text="$t('navigation.info')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/info`" />
        <v-tab
          v-if="canViewData"
          :text="$t('navigation.data')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/data`" />
        <v-tab
          v-if="canViewSchema"
          :text="$t('navigation.schema')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/schema`" />
        <v-tab
          v-if="canViewSettings"
          :text="$t('navigation.settings')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/settings`" />
      </v-tabs>
    </template>
  </v-toolbar>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'
import CreateOntology from '@/components/dialogs/CreateOntology.vue'
import ViewVisibility from '@/components/dialogs/ViewVisibility.vue'

export default {
  components: {
    ViewVisibility,
    CreateOntology
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingDelete: false,
      updateViewDialog: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    pid () {
      return this.$route.query.pid
    },
    database () {
      return this.cacheStore.getDatabase
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    view () {
      return this.cacheStore.getView
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    roles () {
      return this.cacheStore.getRoles
    },
    canViewData () {
      if (!this.view) {
        return false
      }
      if (this.view.is_public) {
        return true
      }
      if (!this.cacheUser) {
        return false
      }
      return this.hasReadAccess || this.view.owner.id === this.cacheUser.uid || this.database.owner.id === this.cacheUser.uid
    },
    canViewSchema () {
      if (!this.view) {
        return false
      }
      if (this.view.is_schema_public) {
        return true
      }
      if (!this.cacheUser) {
        return false
      }
      return this.hasReadAccess || this.view.owner.id === this.cacheUser.uid || this.database.owner.id === this.cacheUser.uid
    },
    canViewSettings () {
      if (!this.cacheUser || !this.view) {
        return false
      }
      return this.view.owner.id === this.cacheUser.uid
    },
    canCreatePid () {
      if (!this.roles || !this.cacheUser || !this.view) {
        return false
      }
      const cacheUserService = useUserService()
      return cacheUserService.hasReadAccess(this.access) && this.roles.includes('create-identifier')
    },
    access () {
      return this.cacheStore.getAccess
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' ||  this.access.type === 'write_own' ||  this.access.type === 'write_all'
    },
    identifiers () {
      if (!this.view) {
        return []
      }
      return this.view.identifiers.filter(s => s.view_id === Number(this.$route.params.view_id))
    },
    identifier () {
      /* mount pid */
      if (this.pid) {
        const filter = this.identifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          const identifier = filter[0]
          console.debug('identifier set according to route pid', identifier)
          return identifier
        }
      }
      return this.identifiers[0]
    },
    title () {
      if (!this.view) {
        return null
      }
      return this.view.name
    }
  }
}
</script>
