<template>
  <v-alert v-if="!canReadData" :type="resourceError ? 'error' : 'info'" variant="tonal" role="status">
    {{ $t(resourceError ? 'dataState.metadataFailed' : !view ? 'dataState.loading' : 'dataState.private') }}
    <v-btn v-if="resourceError" variant="text" @click="retryMetadata">{{ $t('dataState.retry') }}</v-btn>
  </v-alert>
  <div
    v-if="canReadData">
    <ViewToolbar
      v-if="view" />
    <v-toolbar
      color="secondary"
      :title="$t('toolbars.database.current')"
      flat>
      <v-btn
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-download' : null"
        variant="flat"
        :loading="downloadLoading"
        :text="$t('toolbars.table.data.download')"
        class="mr-2"
        @click.stop="download" />
      <v-btn
        :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-refresh' : null"
        variant="flat"
        :text="$t('toolbars.table.data.refresh')"
        class="mr-2"
        :loading="loadingData"
        @click="reload" />
    </v-toolbar>
    <v-card
      rounded="0"
      variant="flat">
      <QueryResults
        id="query-results"
        ref="queryResults"
        :key="resourceKey"
        type="view"
        class="mt-0 mb-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import QueryResults from '@/components/subset/Results.vue'

export default {
  components: {
    QueryResults
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
      cacheStore: useCacheStore()
    }
  },
  computed: {
    resourceError () {
      return this.cacheStore.resourceErrors.view
    },
    resourceKey () {
      return this.canReadData ? `${this.$route.params.database_id}/${this.$route.params.view_id}` : null
    },
    database () {
      return this.cacheStore.getDatabase
    },
    view () {
      const resource = this.cacheStore.getView
      return resource?.id === this.$route.params.view_id && resource.database_id === this.$route.params.database_id ? resource : null
    },
    access () {
      return this.cacheStore.getAccess
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    canReadData () {
      if (!this.view) {
        return false
      }
      if (this.view.is_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
  },
  watch: {
    resourceKey: {
      immediate: true,
      flush: 'post',
      handler (key) {
        if (key) this.$nextTick(() => { if (this.resourceKey === key) this.reload() })
      }
    },
  },
  methods: {
    retryMetadata () {
      return this.cacheStore.setRouteView(this.$route.params.database_id, this.$route.params.view_id)
    },
    reload () {
      if (!this.canReadData || !this.$refs.queryResults) return
      this.$refs.queryResults.reExecute(this.$route.params.view_id)
      this.$refs.queryResults.reExecuteCount(this.$route.params.view_id)
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
        .catch(({code, message}) => {
          this.downloadLoading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.downloadLoading = false
        })
    }
  }
}
</script>
