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
          prepend-icon="mdi-login"
          to="/login">
          {{ $t('navigation.login') }}
        </v-btn>
        <v-btn
          v-if="!user"
          color="primary"
          variant="flat"
          prepend-icon="mdi-account-plus"
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
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
const config = useRuntimeConfig()
useServerHead({
  title: config.public.title
})
</script>
<script>
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  data () {
    return {
      drawer: false,
      model: null,
      query: null,
      loading: true,
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
    table () {
      return this.cacheStore.getTable
    },
    database () {
      return this.cacheStore.getDatabase
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
          return
        }
        /* load database and optional access */
        this.cacheStore.setRouteDatabase(newObj.database_id)
        if (this.user) {
          this.userStore.setRouteAccess(newObj.database_id)
        }
        if (!newObj.table_id) {
          return
        }
        /* load table */
        this.cacheStore.setRouteTable(newObj.database_id, newObj.table_id)
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
