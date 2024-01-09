<template>
  <v-app>
    <!-- Side Bar -->
    <v-navigation-drawer v-model="drawer" fixed app :permanent="$vuetify.breakpoint.lgAndUp">
      <div>
        <v-img
          contain
          class="logo"
          :src="logo" />
      </div>
      <v-list-item class="mt-2">
        <v-list-item-content>
          <v-list-item-subtitle v-text="version" />
          <v-list-item-title class="text-h6" v-text="title" />
        </v-list-item-content>
      </v-list-item>
      <v-list nav>
        <v-list-item
          to="/"
          router>
          <v-list-item-action>
            <v-icon>mdi-information-outline</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('layout.information', { name: 'vue-i18n' }) }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          to="/search"
          router
          exact>
          <v-list-item-action>
            <v-icon>mdi-magnify</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('layout.search', { name: 'vue-i18n' }) }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          v-if="canListOntologies"
          to="/semantic"
          router>
          <v-list-item-action>
            <v-icon>mdi-share-variant</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('layout.semantics', { name: 'vue-i18n' }) }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <div id="messages">
        <v-alert
          v-for="(message, idx) in messages"
          :key="idx"
          class="banner"
          border="left"
          tile
          :type="message.type">
          {{ message.message }}<span v-if="message.link">&nbsp;&mdash;&nbsp;<a :href="message.link" v-text="message.link_text ? message.link_text : message.link" /></span>
        </v-alert>
      </div>
    </v-navigation-drawer>
    <v-form ref="form" @submit.prevent="submit">
      <v-app-bar app extension-height="64">
        <v-app-bar-nav-icon v-if="!$vuetify.breakpoint.lgAndUp" class="mr-1" @click.stop="drawer = !drawer" />
        <!-- Search Bar -->
        <v-text-field
          v-model="search"
          solo
          flat
          single-line
          hide-details
          clearable
          append-icon="mdi-magnify"
          :placeholder="$t('search.fuzzy.placeholder', { name: 'vue-i18n' })"
          @click:append="retrieve" />
        <v-spacer />
        <v-btn
          v-if="!user"
          class="mr-2"
          color="secondary"
          to="/login">
          <v-icon left>mdi-login</v-icon>
          {{ $t('layout.login', { name: 'vue-i18n' }) }}
        </v-btn>
        <v-btn
          v-if="!user"
          color="primary"
          to="/signup">
          <v-icon left>mdi-account-plus</v-icon>
          {{ $t('layout.signup', { name: 'vue-i18n' }) }}
        </v-btn>
        <v-btn
          v-if="user"
          to="/user"
          plain>
          {{ user.username }}
        </v-btn>
        <v-menu v-if="user" bottom offset-y left>
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              icon
              v-bind="attrs"
              v-on="on">
              <v-icon>mdi-dots-vertical</v-icon>
            </v-btn>
          </template>
          <v-list>
            <v-list-item
              v-if="user"
              :to="`/search?t=database&owner.username=${user.username}`">
              {{ $t('layout.mydatabases', { name: 'vue-i18n' }) }}
            </v-list-item>
            <v-list-item
              v-if="user"
              @click="logout">
              {{ $t('layout.logout', { name: 'vue-i18n' }) }}
            </v-list-item>
          </v-list>
        </v-menu>
      </v-app-bar>
    </v-form>
    <v-main>
      <v-container>
        <nuxt />
      </v-container>
    </v-main>
    <script v-if="hasDataset" type="application/ld+json" v-text="datasetJsonLd" />
  </v-app>
</template>

<script>
import DatabaseService from '@/api/database.service'
import TableService from '@/api/table.service'
import DatabaseMapper from '@/api/database.mapper'
import IdentifierMapper from '@/api/identifier.mapper'

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
      search: null
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    locale () {
      return this.$store.state.locale
    },
    messages () {
      return this.$store.state.messages
    },
    table () {
      return this.$store.state.table
    },
    database () {
      return this.$store.state.database
    },
    roles () {
      return this.$store.state.roles
    },
    version () {
      return this.$config.version
    },
    title () {
      return this.$config.title
    },
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
    },
    logo () {
      return this.$config.logo
    },
    hasDataset () {
      return this.$route.path.startsWith('/database')
    },
    datasetJsonLd () {
      if (!this.hasDataset || !this.database) {
        return {}
      }
      if (!('identifiers' in this.database) || this.database.identifiers.length === 0) {
        return DatabaseMapper.databaseToJsonLd(this.database)
      }
      return IdentifierMapper.identifiersToJsonLd(this.database)
    }
  },
  watch: {
    '$i18n.locale': {
      handler () {
        this.$store.commit('SET_LOCALE', this.$i18n.locale)
      }
    },
    '$route.params.database_id': {
      handler (id, oldId) {
        if (id !== oldId) {
          this.loadDatabase()
          this.loadAccess()
        }
      },
      deep: true,
      immediate: true
    },
    '$route.params.table_id': {
      handler (id, oldId) {
        if (id !== oldId) {
          this.loadTable()
        }
      },
      deep: true,
      immediate: true
    }
  },
  mounted () {
    this.initEnvironment()
    this.$store.dispatch('reloadMessages')
    this.$store.dispatch('reloadOntologies')
    if (this.$route.query && this.$route.query.q) {
      this.search = this.$route.query.q
    }
    if (!this.user) {
      return
    }
    this.$vuetify.theme.dark = this.user.attributes.theme_dark
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    login () {
      const redirect = ![undefined, '/', '/login'].includes(this.$router.currentRoute.path)
      this.$router.push({ path: '/login', query: redirect ? { redirect: this.$router.currentRoute.path } : {} })
    },
    logout (message) {
      if (typeof message === 'string') {
        this.$toast.warning(message)
      }
      this.$store.dispatch('logout')
      this.$vuetify.theme.dark = false
      this.$router.push('/database')
    },
    loadDatabase () {
      if (!this.$route.params.database_id) {
        this.$store.commit('SET_DATABASE', null)
        return
      }
      this.loading = true
      DatabaseService.findOne(this.$route.params.database_id)
        .then((database) => {
          this.$store.commit('SET_DATABASE', database)
          this.loading = false
          this.loadTable()
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadTable () {
      if (!this.$route.params.database_id || !this.$route.params.table_id) {
        return
      }
      this.loading = true
      TableService.findOne(this.$route.params.database_id, this.$route.params.table_id)
        .then((table) => {
          this.$store.commit('SET_TABLE', table)
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadAccess () {
      if (!this.$route.params.database_id) {
        return
      }
      this.loading = true
      DatabaseService.checkAccess(this.$route.params.database_id)
        .then((access) => {
          this.$store.commit('SET_ACCESS', access)
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
    },
    retrieve () {
      console.debug('performing fuzzy search')
      this.$router.push({ path: '/search', query: { q: this.search } })
    },
    initEnvironment () {
      this.$store.commit('SET_TITLE', this.$config.title)
      this.$store.commit('SET_ICON', this.$config.icon)
      this.$store.commit('SET_CLIENT_ID', this.$config.clientId)
      this.$store.commit('SET_CLIENT_SECRET', this.$config.clientSecret)
      this.$store.commit('SET_SEARCH_USERNAME', this.$config.searchUsername)
      this.$store.commit('SET_SEARCH_PASSWORD', this.$config.searchPassword)
      this.$store.commit('SET_DOI_URL', this.$config.doiUrl)
      console.debug('runtime config', this.$config)
      if (this.locale) {
        this.$i18n.locale = this.locale
      }
    }
  },
  head () {
    return {
      title: this.$config.title
    }
  }
}
</script>
<style>
#messages {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
}
.banner {
  width: 100%;
  margin: 8px 0 0 0;
}
.search-result-title,
.search-result-subtitle {
  overflow: hidden;
  white-space: pre-line;
}
.v-menu__content {
  max-width: 988px !important;
}
.logo {
  margin: 1em 1em 0;
}
.sl {
  padding-left: 36px;
}
</style>
