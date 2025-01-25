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
          v-if="!loggedIn"
          class="mr-2"
          color="secondary"
          variant="flat"
          :prepend-icon="$vuetify.display.mdAndUp ? 'mdi-login' : null"
          @click="login()">
          {{ $t('navigation.login') }}
        </v-btn>
        <v-btn
          v-if="cacheUser"
          to="/user"
          variant="plain"
          :text="cacheUser.preferred_username" />
        <v-menu
          v-if="loggedIn"
          location="bottom">
          <template v-slot:activator="{ props }">
            <v-btn
              icon="mdi-dots-vertical"
              v-bind="props" />
          </template>
          <v-list>
            <v-list-item
              v-if="cacheUser"
              exact
              :to="`/search?type=database&owner.username=${cacheUser.username}`">
              {{ $t('navigation.databases') + ' ' + $t('navigation.mine')}}
            </v-list-item>
            <v-list-item
              v-if="cacheUser"
              exact
              :to="`/search?type=identifier&identifiers.creator.username=${cacheUser.username}`">
              {{ $t('navigation.identifiers') + ' ' + $t('navigation.mine') }}
            </v-list-item>
            <v-list-item
              @click="logout()">
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
import { useCacheStore } from '@/stores/cache.js'

const { loggedIn, user, login, logout } = useOidcAuth()
const cacheStore = useCacheStore()
cacheStore.setUser(loggedIn ? user.value?.userInfo : null)
cacheStore.setRoles(loggedIn ? user.value?.claims?.realm_access?.roles : [])
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
import { useCacheStore } from '@/stores/cache.js'
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
      cacheStore: useCacheStore()
    }
  },
  computed: {
    messages () {
      return this.cacheStore.getMessages
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
    access () {
      return this.cacheStore.getAccess
    },
    cacheUser () {
      return this.cacheStore.getUser
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
      if (!this.cacheUser) {
        return null
      }
      if (this.table && !this.table.is_public && !this.table.is_schema_public && this.table.owner.id !== this.cacheUser.uid) {
        return makeError(403, null, null)
      }
      if (this.view && !this.view.is_public && !this.view.is_schema_public && this.view.owner.id !== this.cacheUser.uid) {
        return makeError(403, null, null)
      }
      if (this.subset && !this.subset.is_public && !this.subset.is_schema_public && this.subset.owner.id !== this.cacheUser.uid) {
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
      return this.cacheUser
    },
    logo () {
      return this.$config.public.logo
    },
    locale () {
      return this.cacheStore.getLocale
    },
    searchVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : 'solo-filled'
    }
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
          this.cacheStore.setAccess(null)
          return
        }
        if (import.meta.server) {
          return
        }
        /* load database and optional access */
        this.cacheStore.setRouteAccess(newObj.database_id, this.cacheUser?.uid)
        this.cacheStore.setRouteDatabase(newObj.database_id)
          .catch((error) => {
            this.databaseError = error
          })
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
    if (this.$route.query && this.$route.query.q) {
      this.search = this.$route.query.q
    }
    if (!this.cacheUser) {
      return
    }
    this.setTheme()
    this.setLocale()
    this.cacheStore.reloadMessages()
  },
  methods: {
    retrieve () {
      console.debug('performing fuzzy search')
      this.$router.push({ path: '/search', query: { q: this.search } })
    },
    setLocale () {
      if (!this.locale) {
        this.cacheStore.setLocale('en')
        return
      }
      this.$i18n.locale = this.locale
    },
    setTheme () {
      if (!this.cacheUser?.attributes?.theme) {
        return
      }
      switch (this.cacheUser.attributes.theme) {
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
