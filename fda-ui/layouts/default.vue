<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" fixed app :permanent="$vuetify.breakpoint.lgAndUp">
      <v-list-item>
        <v-list-item-content>
          <v-list-item-subtitle>
            {{ version }}
          </v-list-item-subtitle>
          <v-list-item-title class="text-h6">
            Database Repository
          </v-list-item-title>
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
            <v-list-item-title>Information</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          to="/container"
          router>
          <v-list-item-action>
            <v-icon>mdi-database</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Databases</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <div>
        <v-img
          contain
          class="logo"
          :src="logo" />
      </div>
    </v-navigation-drawer>
    <v-app-bar fixed app>
      <v-app-bar-nav-icon v-if="!$vuetify.breakpoint.lgAndUp" class="mr-1" @click.stop="drawer = !drawer" />
      <v-spacer />
      <v-btn
        v-if="!token"
        class="mr-2"
        color="secondary"
        @click="login">
        <v-icon left>mdi-login</v-icon> Login
      </v-btn>
      <v-btn
        v-if="!token"
        class="mr-2"
        color="primary"
        to="/signup">
        <v-icon left>mdi-account-plus</v-icon> Signup
      </v-btn>
      <v-btn v-if="user" to="/user" plain>
        {{ user.username }} <sup v-if="isDeveloper">
          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-icon
                color="primary"
                small
                v-bind="attrs"
                v-on="on">mdi-check-decagram</v-icon>
            </template>
            <span>Developer</span>
          </v-tooltip>
        </sup>
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
            v-for="locale in availableLocales"
            :key="locale.code"
            :to="switchLocalePath(locale.code)">
            <v-list-item-title>{{ locale.name }}</v-list-item-title>
          </v-list-item>
          <v-list-item
            v-if="token"
            @click="logout">
            Logout
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>
    <v-main>
      <v-container>
        <nuxt />
      </v-container>
    </v-main>
    <v-footer
      v-if="sandbox"
      padless>
      <v-card
        flat
        tile
        width="100%"
        class="banner text-center">
        <v-card-text class="black--text">
          This is a <strong>TEST</strong> environment, do not use production/confidential data! — <a href="//github.com/fair-data-austria/dbrepo/issues/new" class="black--text">Report a bug</a>
        </v-card-text>
      </v-card>
    </v-footer>
  </v-app>
</template>

<script>
import { isDeveloper } from '@/utils'

export default {
  name: 'DefaultLayout',
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
      loadingDatabases: false
    }
  },
  computed: {
    availableLocales () {
      // return this.$i18n.locales.filter(i => i.code !== this.$i18n.locale)
      return []
    },
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    container () {
      return this.$store.state.container
    },
    database () {
      return this.$store.state.database
    },
    isDeveloper () {
      return isDeveloper(this.user)
    },
    version () {
      return this.$config.version
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    silentConfig () {
      return {
        headers: this.config.headers,
        progress: false
      }
    },
    sandbox () {
      if (this.$config.sandbox === undefined) {
        console.debug('env sandbox not found, default to', false)
        return false
      }
      console.debug('env sandbox found', this.$config.sandbox)
      return this.$config.sandbox
    },
    logo () {
      return this.$config.logo
    }
  },
  watch: {
    '$route.params.database_id': {
      handler (id, oldId) {
        if (this.user) {
          this.setTheme()
        }
        if (id !== oldId) {
          this.loadDatabase()
          this.loadAccess()
        }
      },
      deep: true,
      immediate: true
    }
  },
  mounted () {
    this.loadUser()
    this.setTheme()
    this.loadDatabase()
    this.loadAccess()
  },
  methods: {
    icon (record) {
      if (!record || !record._source) {
        return null
      }
      switch (record._index) {
        case 'databaseindex':
          return 'mdi-database'
        case 'tableindex':
          return 'mdi-table'
        case 'columnindex':
          return 'mdi-view-column-outline'
        default:
          return 'mdi-lock-clock'
      }
    },
    title (record) {
      if (!record || !record._source) {
        return null
      }
      const { item } = record._source
      return item.name
    },
    subtitle (record) {
      if (!record || !record._source) {
        return null
      }
      const { item } = record._source
      switch (record._index) {
        case 'databaseindex':
          return item.description
        case 'tableindex':
          return item.description
        case 'columnindex':
          return item.columnType
        default:
          return item.description
      }
    },
    link (record) {
      if (!record || !record._source) {
        return null
      }
      const { item } = record._source
      switch (record._index) {
        case 'databaseindex':
          return `/container/${item.id}/database/${item.id}`
        case 'tableindex':
          return `/container/${item.tdbid}/database/${item.tdbid}/table/${item.id}`
        case 'columnindex':
          return `/container/${item.cdbid}/database/${item.cdbid}/table/${item.tid}`
        default:
          return `/pid/${item.id}`
      }
    },
    login () {
      const redirect = ![undefined, '/', '/login'].includes(this.$router.currentRoute.path)
      this.$router.push({ path: '/login', query: redirect ? { redirect: this.$router.currentRoute.path } : {} })
    },
    navigate (item) {
      this.$router.push(this.link(item))
    },
    logout () {
      this.$store.commit('SET_TOKEN', null)
      this.$store.commit('SET_USER', null)
      this.$store.commit('SET_ACCESS', null)
      this.$vuetify.theme.dark = false
      this.$router.push('/container')
    },
    async searchIndizes () {
      this.loadingSearch = true
      try {
        const res = await this.$axios.get('/search/databaseindex,tableindex,columnindex/_search?q=*&terminate_after=50')
        const { hits } = res.data
        console.info('search results', hits.total.value)
        console.debug('search results', hits.hits)
        if (!hits || !hits.hits) {
          this.searchResults = []
        } else {
          this.searchResults = hits.hits
        }
      } catch (err) {
        console.error('Failed to load search results', err)
      }
      this.loadingSearch = false
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      try {
        this.loadingUser = true
        const res = await this.$axios.put('/api/auth', {}, this.config)
        this.$store.commit('SET_USER', res.data)
        console.debug('user information', this.user)
        this.$vuetify.theme.dark = this.user.theme_dark
        this.loading = false
      } catch (err) {
        const { status } = err.response
        if (status === 401) {
          console.error('Token expired', err)
          this.$toast.warning('Login has expired')
          this.logout()
        } else {
          console.error('user data', err)
          this.$toast.error('Failed to load user')
          this.error = true
        }
      }
      this.loadingUser = false
    },
    async loadDatabase () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.$store.commit('SET_DATABASE', res.data)
        console.debug('database', this.database)
      } catch (err) {
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
    },
    async loadAccess () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        return
      }
      if (!this.token) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access`, this.silentConfig)
        this.access = res.data
        this.$store.commit('SET_ACCESS', res.data)
        console.debug('access', this.access)
      } catch (err) {
        const { status } = err.response
        if (status !== 401 && status !== 403) {
          console.error('Failed to check access', err)
          this.$toast.error('Failed to check access')
        }
      }
      this.loading = false
    },
    setTheme () {
      if (!this.user || !this.user.theme_dark) {
        this.$vuetify.theme.dark = false
        return
      }
      this.$vuetify.theme.dark = this.user.theme_dark
    }
  }
}
</script>
<style>
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
