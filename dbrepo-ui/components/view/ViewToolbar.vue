<template>
  <v-toolbar flat>
    <v-btn
      class="mr-2"
      size="small"
      icon="mdi-arrow-left"
      :to="`/database/${$route.params.database_id}/view`" />
    <v-toolbar-title
      v-if="view"
      :text="title" />
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
      v-if="canCreatePid"
      class="mr-2"
      :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-content-save-outline' : null"
      variant="flat"
      color="primary"
      :text="($vuetify.display.lgAndUp ? $t('toolbars.view.pid.xl') + ' ' : '') + $t('toolbars.view.pid.permanent')"
      :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/persist`" />
    <template v-slot:extension>
      <v-tabs
        v-model="tab"
        color="primary">
        <v-tab
          :text="$t('navigation.info')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/info`" />
        <v-tab
          :text="$t('navigation.data')"
          :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/data`" />
      </v-tabs>
    </template>
  </v-toolbar>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingDelete: false,
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
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    canDeleteView () {
      if (!this.roles || !this.user || !this.view || !this.view.creator) {
        return false
      }
      return this.roles.includes('delete-database-view') && this.view.creator.id === this.user.id
    },
    canCreatePid () {
      if (!this.roles || !this.user || !this.view) {
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
    }
  }
}
</script>
