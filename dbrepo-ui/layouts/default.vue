<template>
  <v-app>
    <!-- Side Bar -->
    <v-navigation-drawer
      v-model="drawer"
      fixed
      app
      :permanent="$vuetify.display.lgAndUp">
      <NuxtLink to="/">
        <v-img
          contain
          alt="organization logo"
          class="logo"
          style="margin:1em;"
          :src="logo" />
      </NuxtLink>
      <v-list-item
        class="mt-2">
        <v-list-item-title
          class="text-h6">
          {{ title }}
        </v-list-item-title>
      </v-list-item>
      <v-list nav>
        <v-list-item
          to="/"
          prepend-icon="mdi-information-outline"
          :title="$t('navigation.information')" />
        <v-list-item
          to="/search"
          exact
          prepend-icon="mdi-magnify"
          :title="$t('navigation.search')" />
        <v-list-item
          v-if="canListOntologies"
          to="/semantic"
          prepend-icon="mdi-share-variant"
          :title="$t('navigation.semantics')" />
        <v-list-item
          v-if="canListContainers"
          to="/container"
          prepend-icon="mdi-database-settings"
          :title="$t('navigation.container')" />
      </v-list>
      <template v-slot:append>
        <v-alert
          v-for="(message, idx) in messages"
          :key="idx"
          class="banner"
          border="start"
          tile
          :type="message.type">
          {{ message.message }}<span v-if="message.link">&nbsp;&mdash;&nbsp;<a :href="message.link">{{ message.link_text ? message.link_text : message.link }}</a></span>
        </v-alert>
        <div class="d-flex pa-2">
          <v-spacer />
          <v-btn
            variant="plain"
            :text="commitShort"
            size="x-small"
            prepend-icon="mdi-source-commit"
            :href="`https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commit/${commit}`" />
          <v-btn
            variant="plain"
            prepend-icon="mdi-tag"
            :href="`https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/${version}`"
            :text="version"
            size="x-small" />
        </div>
      </template>
    </v-navigation-drawer>
    <v-form
      ref="form"
      @submit.prevent="retrieve">
      <v-app-bar
        app
        flat
        class="pr-1"
        extension-height="64">
        <template v-slot:prepend>
          <v-app-bar-nav-icon
            class="mr-3"
            @click.stop="drawer = !drawer" />
        </template>
        <!-- Search Bar -->
        <v-text-field
          class="fuzzy-search"
          v-model="search"
          :variant="searchVariant"
          flat
          single-line
          hide-details
          clearable
          append-inner-icon="mdi-magnify"
          :placeholder="$t('toolbars.search.fuzzy.placeholder')"
          @click:append-inner="retrieve" />
        <v-spacer />
        <v-btn
          v-if="!user"
          class="mr-2"
          color="secondary"
          variant="flat"
          :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-login' : null"
          to="/login">
          {{ $t('navigation.login') }}
        </v-btn>
        <v-btn
          v-if="!user"
          color="primary"
          variant="flat"
          :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-account-plus' : null"
          to="/signup">
          {{ $t('navigation.signup') }}
        </v-btn>
        <v-btn
          v-if="user"
          to="/user"
          variant="plain"
          :text="user.username" />
        <v-menu
          v-if="user"
          location="bottom">
          <template v-slot:activator="{ props }">
            <v-btn
              icon="mdi-dots-vertical"
              v-bind="props" />
          </template>
          <v-list>
            <v-list-item
              v-if="user"
              exact
              :to="`/search?type=database&owner.username=${user.username}`">
              {{ $t('navigation.databases') + ' ' + $t('navigation.mine')}}
            </v-list-item>
            <v-list-item
              v-if="user"
              exact
              :to="`/search?type=identifier&identifiers.creator.username=${user.username}`">
              {{ $t('navigation.identifiers') + ' ' + $t('navigation.mine') }}
            </v-list-item>
            <v-list-item
              v-if="user"
              @click="logout">
              {{ $t('navigation.logout') }}
            </v-list-item>
          </v-list>
        </v-menu>
      </v-app-bar>
    </v-form>
    <v-main>
      <v-container>
        <slot />
        <JumboBox
          v-if="error"
          :title="$t(errorCodeKey(error).title, { resource })"
          :subtitle="$t(errorCodeKey(error).subtitle)"
          :text="$t(errorCodeKey(error).text, { resource })" />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref } from 'vue'

const runtimeConfig = useRuntimeConfig()
const config = ref(runtimeConfig)
useServerHead({
  title: runtimeConfig.public.title,
  meta: [
    { 'ref': 'icon', type: 'image/x-icon', href: runtimeConfig.public.icon },
    { 'http-equiv': 'Content-Security-Policy', content: 'upgrade-insecure-requests' }
  ]
})
</script>
<script>
import JumboBox from '@/components/JumboBox.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import { errorCodeKey, makeError } from '@/utils'

export default {
  components: {
    JumboBox
  },
  data () {
    return {
      drawer: false,
      model: null,
      query: null,
      loading: true,
      databaseError: null,
      accessError: null,
      searchResults: [],
      databases: [],
      loadingUser: true,
      loadingSearch: false,
      loadingDatabases: false,
      search: null,
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    token () {
      return this.userStore.getToken
    },
    user () {
      return this.userStore.getUser
    },
    locale () {
      return this.userStore.getLocale
    },
    messages () {
      return this.cacheStore.getMessages
    },
    access () {
      return this.userStore.getAccess
    },
    table () {
      return this.cacheStore.getTable
    },
    view () {
      return this.cacheStore.getView
    },
    subset () {
      return this.cacheStore.getSubset
    },
    database () {
      return this.cacheStore.getDatabase
    },
    resource () {
      if (!this.$route.params.database_id) {
        return null
      }
      if (this.$route.params.table_id) {
        return 'table'
      }
      if (this.$route.params.view_id) {
        return 'view'
      }
      if (this.$route.params.subset_id) {
        return 'subset'
      }
      return 'database'
    },
    roles () {
      return this.userStore.getRoles
    },
    version () {
      return this.$config.public.version
    },
    title () {
      return this.$config.public.title
    },
    commit () {
      return this.$config.public.commit
    },
    commitShort () {
      return this.$config.public.commit.substr(0, 8)
    },
    error () {
      if (this.databaseError) {
        return this.databaseError
      }
      if (this.accessError) {
        return this.accessError
      }
      if (!this.user) {
        return null
      }
      if (this.table && !this.table.is_public && !this.table.is_schema_public && !this.table.owner.id !== this.user.id) {
        return makeError(403, null, null)
      }
      if (this.view && !this.view.is_public && !this.view.is_schema_public && !this.view.owner.id !== this.user.id) {
        return makeError(403, null, null)
      }
      if (this.subset && !this.subset.is_public && !this.subset.is_schema_public && !this.subset.owner.id !== this.user.id) {
        return makeError(403, null, null)
      }
      return null
    },
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
    },
    canListContainers () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-containers')
    },
    logo () {
      return this.$config.public.logo
    },
    searchVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : 'solo-filled'
    },
  },
  watch: {
    '$route.params': {
      handler (newObj, oldObj) {
        if (!newObj.database_id) {
          this.databaseError = null
          this.accessError = null
          this.cacheStore.setTable(null)
          this.cacheStore.setView(null)
          this.cacheStore.setSubset(null)
          return
        }
        if (import.meta.server) {
          return
        }
        /* load database and optional access */
        this.cacheStore.setRouteDatabase(newObj.database_id)
          .catch((error) => {
            this.databaseError = error
          })
        if (this.user) {
          this.userStore.setRouteAccess(newObj.database_id)
        }
        /* load table */
        if (newObj.table_id) {
          this.cacheStore.setRouteTable(newObj.database_id, newObj.table_id)
        } else {
          this.cacheStore.setTable(null)
        }
        /* load view */
        if (newObj.view_id) {
          this.cacheStore.setRouteView(newObj.database_id, newObj.view_id)
        } else {
          this.cacheStore.setView(null)
        }
        /* load subset */
        if (newObj.subset_id) {
          this.cacheStore.setRouteSubset(newObj.database_id, newObj.subset_id)
        } else {
          this.cacheStore.setSubset(null)
        }
      },
      deep: true,
      immediate: true
    }
  },
  mounted () {
    this.initEnvironment()
    if (this.$route.query && this.$route.query.q) {
      this.search = this.$route.query.q
    }
    if (!this.user) {
      return
    }
    this.setTheme()
    this.cacheStore.reloadMessages()
  },
  methods: {
    errorCodeKey,
    login () {
      const redirect = ![undefined, '/', '/login'].includes(this.$router.currentRoute.path)
      this.$router.push({ path: '/login', query: redirect ? { redirect: this.$router.currentRoute.path } : {} })
    },
    logout () {
      this.$vuetify.theme.global.name = 'tuwThemeLight'
      this.userStore.logout()
      this.$router.push('/database')
    },
    retrieve () {
      console.debug('performing fuzzy search')
      this.$router.push({ path: '/search', query: { q: this.search } })
    },
    initEnvironment () {
      if (this.token && !this.user) {
        console.error('Something went wrong with loading the user: reset user cache')
        this.userStore.logout()
      }
      if (!this.locale) {
        this.userStore.setLocale('en')
      }
      this.$i18n.locale = this.locale
    },
    setTheme () {
      switch (this.user.attributes.theme) {
        case 'dark':
          this.$vuetify.theme.global.name = 'tuwThemeDark'
          break
        case 'light':
          this.$vuetify.theme.global.name = 'tuwThemeLight'
          break
        case 'light-contrast':
          this.$vuetify.theme.global.name = 'tuwThemeLightContrast'
          break
        case 'dark-contrast':
          this.$vuetify.theme.global.name = 'tuwThemeDarkContrast'
          break
      }
    },
    setLocale (code) {
      this.userStore.setLocale(code)
      this.$i18n.locale = this.locale
    }
  }
}
</script>
<style lang="scss">
.v-menu__content {
  max-width: 988px !important;
}
.sl {
  padding-left: 36px;
}
</style>
