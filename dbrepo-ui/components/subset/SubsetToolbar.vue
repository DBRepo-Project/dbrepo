<template>
  <div>
    <v-toolbar flat>
      <v-btn
        class="mr-2"
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/subset`" />
      <v-toolbar-title
        v-if="identifier"
        :text="title" />
      <v-spacer />
      <v-btn
        v-if="result_visibility && subset && subset.result_number"
        class="mb-1 ml-2"
        color="tertiary"
        :variant="buttonVariant"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-download' : null"
        :loading="downloadLoading"
        @click.stop="downloadSubset">
        {{ ($vuetify.display.lgAndUp ? $t('toolbars.subset.export.data.xl') + ' ' : '') + $t('toolbars.subset.export.data.permanent') }}
      </v-btn>
      <v-btn
        v-if="canPersistQuery"
        :loading="loadingSave"
        color="secondary"
        variant="flat"
        class="mb-1 ml-2"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-content-save-outline' : null"
        :text="$t('toolbars.subset.save.permanent')"
        @click.stop="save" />
      <v-btn
        v-if="canForgetQuery"
        :loading="loadingSave"
        color="error"
        variant="flat"
        class="mb-1 ml-2"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-trash-can-outline' : null"
        :text="$t('toolbars.subset.unsave.permanent')"
        @click.stop="forget" />
      <v-btn
        v-if="canGetPid"
        class="mb-1 ml-2"
        color="primary"
        variant="flat"
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-content-save-outline' : null"
        :disabled="!executionUTC"
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
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
      subset: null,
      userStore: useUserStore(),
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
      return this.userStore.getAccess
    },
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    identifiers () {
      if (!this.database || !this.database.subsets || this.database.subsets.length === 0) {
        return []
      }
      return this.database.subsets.filter(s => s.query_id === Number(this.$route.params.subset_id))
    },
    canViewData () {
      if (!this.database) {
        return false
      }
      return this.database.is_public
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
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
    },
    result_visibility () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      return this.subset.creator.username === this.username
    },
    canGetPid () {
      if (!this.user || !this.subset || !this.database) {
        return false
      }
      return this.database.owner.id === this.user.id || (this.subset.creator.id === this.user.id && UserUtils.hasReadAccess(this.access))
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
  mounted () {
    /* load subset metadata */
    if (!this.subset) {
      this.loadSubset()
    }
  },
  methods: {
    save () {
      this.loadingSave = true
      const queryService = useQueryService()
      queryService.update(this.$route.params.database_id, this.$route.params.subset_id, { persist: true })
        .then((subset) => {
          this.subset = subset
          this.loadingSave = false
        })
        .catch(() => {
          this.loadingSave = false
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    forget () {
      this.loadingSave = true
      const queryService = useQueryService()
      queryService.update(this.$route.params.database_id, this.$route.params.subset_id, { persist: false })
        .then((subset) => {
          this.subset = subset
        })
        .catch(() => {
          this.loadingSave = false
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    loadSubset () {
      this.loading = true
      const queryService = useQueryService()
      queryService.findOne(this.$route.params.database_id, this.$route.params.subset_id)
        .then((subset) => {
          this.subset = subset
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    downloadSubset () {
      this.downloadLoading = true
      const queryService = useQueryService()
      queryService.exportCsv(this.$route.params.database_id, this.$route.params.subset_id)
        .then((data) => {
          const url = URL.createObjectURL(data)
          const link = document.createElement('a')
          link.href = url
          link.download = 'subset.csv'
          document.body.appendChild(link)
          link.click()
        })
        .catch(() => {
          this.downloadLoading = false
        })
        .finally(() => {
          this.downloadLoading = false
        })
    }
  }
}
</script>
