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
      v-cloak
      :loading="loading"
      :databases="databases" />
    <div class="text-center">
      <v-btn
        v-if="databases && databases.length > 0"
        class="mt-2"
        variant="flat"
        to="/search"
        color="plain">
        {{ $t('navigation.more')}}
      </v-btn>
    </div>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
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
      databases: [],
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
      return `${this.databases.length} ${this.$t('toolbars.dashboard.databases')}`
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
        .then((databases) => {
          this.databases = databases
          this.loading = false
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    },
  }
}
</script>
