<template>
  <div v-if="view">
    <v-toolbar flat>
      <v-btn
        class="mr-2"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/view`" />
      <v-toolbar-title
        :text="title" />
      <v-spacer />
      <v-btn
        v-if="canDeleteView"
        prepend-icon="mdi-delete"
        class="mr-2"
        variant="flat"
        color="error"
        :text="$vuetify.display.lgAndUp ? $t('navigation.delete') : ''"
        :loading="loadingDelete"
        @click="deleteView" />
      <v-btn
        v-if="canCreatePid"
        prepend-icon="mdi-content-save-outline"
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
  </div>
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
        .catch(({code}) => {
          const toast = useToastInstance()
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingDelete = false
        })
    }
  }
}
</script>
