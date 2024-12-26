<template>
  <v-toolbar flat>
    <v-btn
      class="mr-2"
      size="small"
      icon="mdi-arrow-left"
      :to="`/database/${$route.params.database_id}/view`" />
    <v-toolbar-title
      v-if="cachedView">
      <span
        v-if="$vuetify.display.lgAndUp">
        {{ title }}
      </span>
      <v-chip
        v-if="cachedView.is_public"
        size="small"
        class="ml-2"
        color="success"
        :text="$t('toolbars.database.public')"
        variant="outlined" />
      <v-chip
        v-if="!cachedView.is_public"
        size="small"
        class="ml-2"
        :color="colorVariant"
        variant="outlined"
        :text="$t('toolbars.database.private')"
        flat />
    </v-toolbar-title>
    <v-spacer />
    <v-btn
      v-if="canDeleteView"
      class="mr-2"
      variant="flat"
      :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-delete' : null"
      :loading="loadingDelete"
      color="error"
      :text="$t('navigation.delete')"
      @click="deleteView" />
    <v-btn
      v-if="canUpdateVisibility"
      class="mr-2"
      variant="flat"
      :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-eye' : null"
      color="warning"
      :text="$t('navigation.visibility')"
      @click="updateViewDialog = true" />
    <v-btn
      v-if="canCreatePid"
      class="mr-2"
      :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-content-save-outline' : null"
      variant="flat"
      color="primary"
      :text="($vuetify.display.lgAndUp ? $t('toolbars.view.pid.xl') + ' ' : '') + $t('toolbars.view.pid.permanent')"
      :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/persist`" />
    <v-dialog
      v-model="updateViewDialog"
      persistent
      max-width="640">
      <ViewVisibility
        :view="cachedView"
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
          v-if="canReadData"
          :text="$t('navigation.data')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/data`" />
        <v-tab
          v-if="canReadData"
          :text="$t('navigation.schema')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/schema`" />
      </v-tabs>
    </template>
  </v-toolbar>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import CreateOntology from "@/components/dialogs/CreateOntology.vue"
import ViewVisibility from "@/components/dialogs/ViewVisibility.vue"

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
      userStore: useUserStore(),
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
    cachedView () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    canViewData () {
      if (!this.cachedView) {
        return false
      }
      if (this.cachedView.is_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.hasReadAccess || this.cachedView.owned_by === this.user.id || this.database.owner.id === this.user.id
    },
    canViewSchema () {
      if (!this.cachedView) {
        return false
      }
      if (this.cachedView.is_schema_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.hasReadAccess || this.cachedView.owned_by === this.user.id || this.database.owner.id === this.user.id
    },
    canDeleteView () {
      if (!this.roles || !this.user || !this.cachedView) {
        return false
      }
      return this.roles.includes('delete-database-view') && this.cachedView.owned_by === this.user.id
    },
    canUpdateVisibility () {
      if (!this.roles || !this.user || !this.cachedView) {
        return false
      }
      return this.roles.includes('modify-view-visibility') && this.cachedView.owned_by === this.user.id
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
    canCreatePid () {
      if (!this.roles || !this.user || !this.cachedView) {
        return false
      }
      const userService = useUserService()
      return this.roles.includes('create-identifier') && userService.hasReadAccess(this.access)
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
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' ||  this.access.type === 'write_own' ||  this.access.type === 'write_all'
    },
    canReadData () {
      if (!this.cachedView) {
        return false
      }
      if (this.cachedView.is_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.cachedView.owner.id === this.user.id || this.hasReadAccess
    },
    identifiers () {
      if (!this.cachedView) {
        return []
      }
      return this.cachedView.identifiers.filter(s => s.view_id === Number(this.$route.params.view_id))
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
      if (!this.cachedView) {
        return null
      }
      return this.cachedView.name
    }
  },
  methods: {
    deleteView () {
      this.loadingDelete = true
      const viewService = useViewService()
      viewService.remove(this.$route.params.database_id, this.$route.params.view_id)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.view.delete'))
          this.cacheStore.reloadDatabase()
          this.$router.push(`/database/${this.$route.params.database_id}/view`)
        })
        .catch(({code, message}) => {
          const toast = useToastInstance()
          if (typeof code !== 'string' || typeof message !== 'string') {
            return
          }
          toast.error(this.$t(code) + ": " + message)
        })
        .finally(() => {
          this.loadingDelete = false
        })
    },
    close ({success}) {
      this.updateViewDialog = false
      if (success) {
        this.cacheStore.reloadDatabase()
      }
    }
  }
}
</script>
