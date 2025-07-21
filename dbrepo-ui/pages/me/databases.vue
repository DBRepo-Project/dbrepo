<template>
  <div
    v-if="loggedIn">
    <v-toolbar
      flat>
      <v-toolbar-title
        v-if="cacheUser">
        {{ header }}
      </v-toolbar-title>
    </v-toolbar>
    <DatabaseList
      :loading="loading"
      :databases="results" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script setup>
const { loggedIn } = useOidcAuth()
</script>
<script>
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      tab: 0,
      loading: false,
      results: [],
      searchData: {},
      items: [
        {
          title: this.$t('navigation.dashboard'),
          to: '/me'
        },
        {
          title: this.$t('navigation.databases'),
          to: `/me/databases`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    locale () {
      return this.cacheStore.getLocale
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    header () {
      return `${this.results.length} ${this.$t('toolbars.dashboard.databases')}`
    }
  },
  mounted () {
    this.getMyDatabases()
  },
  methods: {
    getMyDatabases () {
      this.loading = true
      const searchService = useSearchService()
      searchService.general_search('database', this.searchData)
        .then((results) => {
          this.results = results
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
    },
  }
}
</script>
