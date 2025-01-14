<template>
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
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'view' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'view' })" />
</template>

<script setup>
import { ref } from 'vue'

const runtimeConfig = useRuntimeConfig()
const config = ref(runtimeConfig)
</script>
<script>
import TimeDrift from '@/components/TimeDrift.vue'
import JumboBox from '@/components/JumboBox.vue'
import QueryResults from '@/components/subset/Results.vue'
import { useUserStore } from '@/stores/user'
import { errorCodeKey } from '@/utils'

export default {
  components: {
    QueryResults,
    TimeDrift,
    JumboBox
  },
  setup () {
    const userStore = useUserStore()
    const { database_id, view_id } = useRoute().params
    const { error } = useFetch(`${this.config.public.api.server}/api/database/${database_id}/view/${view_id}`, {
      immediate: true,
      method: 'HEAD',
      timeout: 90_000,
      headers: {
        Accept: 'application/json',
        Authorization: userStore.getToken ? `Bearer ${userStore.getToken}` : null
      }
    })
    return {
      error
    }
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
    view () {
      return this.cacheStore.getView
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
      if (!this.view) {
        return false
      }
      if (this.view.is_public) {
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
    errorCodeKey,
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
          if (typeof code !== 'string') {
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
