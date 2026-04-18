<template>
  <div>
    <v-toolbar
      flat>
      <v-btn
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/subset`" />
      <v-toolbar-title
        v-if="identifier"
        :text="title" />
      <v-spacer />
      <v-btn
        v-if="canViewSubsetData"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-download' : null"
        variant="flat"
        :loading="downloadLoading"
        :text="$t('toolbars.table.data.download')"
        class="mr-2"
        @click.stop="download" />
      <v-btn
        v-if="canPersistQuery"
        :loading="loadingSave"
        color="secondary"
        variant="flat"
        class="mr-2"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-star' : null"
        :text="$t('toolbars.subset.save.permanent')"
        @click.stop="save" />
      <v-btn
        v-if="canForgetQuery"
        :loading="loadingSave"
        color="warning"
        variant="flat"
        class="mr-2"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-star-off' : null"
        :text="$t('toolbars.subset.unsave.permanent')"
        @click.stop="forget" />
      <v-btn
        v-if="canGetPid"
        color="primary"
        variant="flat"
        class="mr-2"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-identifier' : null"
        :to="`/database/${$route.params.database_id}/subset/${$route.params.subset_id}/persist`">
        {{ ($vuetify.display.lgAndUp ? $t('toolbars.subset.pid.xl') + ' ' : '') + $t('toolbars.subset.pid.permanent') }}
      </v-btn>
      <template v-slot:extension>
        <v-tabs
          v-model="tab"
          color="primary">
          <v-tab
            :text="$t('navigation.info')"
            :to="`/database/${$route.params.database_id}/subset/${$route.params.subset_id}/info`" />
          <v-tab
            v-if="canViewData"
            :text="$t('navigation.data')"
            :to="`/database/${$route.params.database_id}/subset/${$route.params.subset_id}/data`" />
        </v-tabs>
      </template>
    </v-toolbar>
  </div>
</template>

<script>
import DownloadButton from '@/components/identifier/DownloadButton.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    DownloadButton
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingSave: false,
      downloadLoading: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    pid () {
      if (!this.$route.subset) {
        return null
      }
      return this.$route.subset.pid
    },
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.cacheStore.getAccess
    },
    subset () {
      return this.cacheStore.getSubset
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    identifiers () {
      if (!this.subset) {
        return []
      }
      return this.subset.identifiers
    },
    canViewData () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      return this.access
    },
    identifier () {
      /* mount pid */
      if (this.pid) {
        const filter = this.identifiers.filter(i => i.id === this.pid)
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.identifiers[0]
    },
    canPersistQuery () {
      if (this.loading || !this.subset || this.subset.is_persisted) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    canForgetQuery () {
      if (this.loading || !this.subset || !this.subset.is_persisted) {
        return false
      }
      if (this.subset.identifiers.length > 0) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    canViewSubsetData () {
      if (!this.database || !this.subset) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    canGetPid () {
      if (!this.cacheUser || !this.subset || !this.database) {
        return false
      }
      return this.database.owner.username === this.cacheUser.preferred_username || (this.subset.owner.username === this.cacheUser.preferred_username && this.hasReadAccess)
    },
    title () {
      if (!this.identifier) {
        return null
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierPreferEnglishTitle(this.identifier)
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  methods: {
    save () {
      this.loadingSave = true
      const queryService = useQueryService()
      queryService.update(this.$route.params.database_id, this.$route.params.subset_id, { persist: true })
        .then(() => {
          const cacheStore = useCacheStore()
          cacheStore.reloadSubset()
          this.loadingSave = false
        })
        .catch(({code, message}) => {
          this.loadingSave = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    forget () {
      this.loadingSave = true
      const queryService = useQueryService()
      queryService.update(this.$route.params.database_id, this.$route.params.subset_id, { persist: false })
        .then(() => {
          const cacheStore = useCacheStore()
          cacheStore.reloadSubset()
          this.loadingSave = false
        })
        .catch(({code, message}) => {
          this.loadingSave = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    download () {
      this.downloadLoading = true
      const queryService = useQueryService()
      queryService.exportCsv(this.$route.params.database_id, this.subset.id)
        .then((data) => {
          this.downloadLoading = false
          const url = URL.createObjectURL(data)
          const link = document.createElement('a')
          link.href = url
          link.download = 'subset.csv'
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
