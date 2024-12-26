<template>
  <div
    v-if="canReadData">
    <ViewToolbar
      v-if="cachedView" />
    <v-toolbar
      color="secondary"
      :title="$t('toolbars.database.current')"
      flat>
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-download' : null"
        variant="flat"
        :loading="downloadLoading"
        :text="$t('toolbars.table.data.download')"
        class="mr-2"
        @click.stop="download" />
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-refresh' : null"
        variant="flat"
        :text="$t('toolbars.table.data.refresh')"
        class="mr-2"
        :loading="loadingData"
        @click="reload" />
    </v-toolbar>
    <TimeDrift />
    <v-card tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
        type="view"
        class="mt-0 mb-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import TimeDrift from '@/components/TimeDrift.vue'
import QueryResults from '@/components/subset/Results.vue'

export default {
  components: {
    QueryResults,
    TimeDrift
  },
  data () {
    return {
      loadingData: false,
      downloadLoading: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'},
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}`
        },
        {
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/view`
        },
        {
          title: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
        },
        {
          title: this.$t('navigation.data'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/data`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore(),
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    cachedView () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    access () {
      return this.userStore.getAccess
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
      return this.view.owner.id === this.user.id || this.hasReadAccess
    },
  },
  mounted () {
    if (!this.canReadData) {
      return
    }
    this.reload()
  },
  methods: {
    reload () {
      this.$refs.queryResults.reExecute(Number(this.$route.params.view_id))
      this.$refs.queryResults.reExecuteCount(Number(this.$route.params.view_id))
    },
    download () {
      this.downloadLoading = true
      const viewService = useViewService()
      viewService.exportData(this.$route.params.database_id, this.$route.params.view_id)
        .then((data) => {
          this.downloadLoading = false
          const url = URL.createObjectURL(data)
          const link = document.createElement('a')
          link.href = url
          link.download = 'view.csv'
          document.body.appendChild(link)
          link.click()
        })
        .catch(({code}) => {
          this.downloadLoading = false
          const toast = useToastInstance()
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.downloadLoading = false
        })
    }
  }
}
</script>
