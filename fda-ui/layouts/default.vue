<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" fixed app :permanent="$vuetify.breakpoint.lgAndUp">
      <div>
        <v-img
          contain
          class="logo"
          :src="logo" />
      </div>
      <v-list-item class="mt-2">
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
    </v-navigation-drawer>
    <v-form ref="form" @submit.prevent="submit">
      <v-app-bar fixed app>
        <v-app-bar-nav-icon v-if="!$vuetify.breakpoint.lgAndUp" class="mr-1" @click.stop="drawer = !drawer" />
        <v-text-field
          v-model="search"
          solo
          flat
          single-line
          hide-details
          placeholder="Search ..." />
        <v-btn icon class="ml-2" type="submit" name="search-submit" @click="retrieve">
          <v-icon>mdi-magnify</v-icon>
        </v-btn>
        <v-spacer />
        <v-btn
          v-if="!token"
          class="mr-2"
          color="secondary"
          @click="login">
          <v-icon left>mdi-login</v-icon> Login
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
    </v-form>
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
import { isDeveloper, jwtToUser } from '@/utils'
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
      loadingDatabases: false,
      search: null,
      refreshToken: {
        client_id: 'dbrepo-client',
        grant_type: 'refresh_token',
        client_secret: this.$config.client_secret,
        refresh_token: null
      }
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
    table () {
      return this.$store.state.table
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
    keycloakConfig () {
      return {
        headers: {
          Authorization: `Bearer ${this.token}`,
          ContentType: 'application/x-www-form-urlencoded'
        }
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
    this.loadUser()
    this.setTheme()
    this.loadDatabase()
      .then(() => {
        this.loadIdentifier()
        this.loadTable()
      })
    this.loadAccess()
    if (this.$route.query && this.$route.query.q) {
      this.search = this.$route.query.q
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    login () {
      const redirect = ![undefined, '/', '/login'].includes(this.$router.currentRoute.path)
      this.$router.push({ path: '/login', query: redirect ? { redirect: this.$router.currentRoute.path } : {} })
    },
    logout () {
      this.$store.commit('SET_TOKEN', null)
      this.$store.commit('SET_REFRESH_TOKEN', null)
      this.$store.commit('SET_USER', null)
      this.$store.commit('SET_ACCESS', null)
      this.$vuetify.theme.dark = false
      this.$router.push('/container')
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      try {
        this.loadingUser = true
        const res = await this.$axios.get('/api/auth/realms/dbrepo/protocol/openid-connect/userinfo', this.keycloakConfig)
        const user = jwtToUser(res.data)
        console.debug('user information', user)
        this.$store.commit('SET_USER', user)
        this.$vuetify.theme.dark = user.theme_dark
        this.loading = false
      } catch (err) {
        console.error('Failed to load user', err)
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
    async loadTable () {
      if (!this.$route.params.container_id || !this.$route.params.database_id || !this.$route.params.table_id) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`, this.config)
        this.$store.commit('SET_TABLE', res.data)
        console.debug('table', this.table)
      } catch (error) {
        const { status } = error.response
        if (status === 405) {
          const table = this.database.tables.filter(t => t.id === Number(this.$route.params.table_id))[0]
          this.$store.commit('SET_TABLE', table)
        } else {
          const { message } = error.response.data
          console.error('Failed to load table', error)
          this.$toast.error(`Failed to load table: ${message}`)
        }
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
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access`, this.config)
        this.access = res.data
        this.$store.commit('SET_ACCESS', res.data)
        console.debug('access', this.access)
      } catch (err) {
        this.$store.commit('SET_ACCESS', null)
        const { status } = err.response
        if (status !== 401 && status !== 403) {
          console.error('Failed to check access', err)
          this.$toast.error('Failed to check access')
        }
      }
      this.loading = false
    },
    async loadIdentifier () {
      if (!this.database || 'identifier' in this.database) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/pid/${this.database.identifier.id}`, this.config)
        const db = this.database
        db.identifier = res.data
        this.$store.commit('SET_DATABASE', db)
      } catch (err) {
        console.error('Failed to load identifier', err)
        this.$toast.error('Failed to load identifier')
      }
      this.loading = false
    },
    retrieve () {
      this.$router.push({ path: '/search', query: { q: this.search } })
    },
    setTheme () {
      if (!this.user || !this.user.theme_dark) {
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
