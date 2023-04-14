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
        <div v-if="!user">
          <v-btn
            class="mr-2"
            color="secondary"
            to="/login">
            <v-icon left>mdi-login</v-icon> Login
          </v-btn>
          <v-btn
            class="mr-2"
            color="primary"
            to="/signup">
            <v-icon left>mdi-account-plus</v-icon> Signup
          </v-btn>
        </div>
        <div v-else>
          <v-btn to="/user" plain>
            {{ user.username }}
          </v-btn>
          <v-menu bottom offset-y left>
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
        </div>
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
import { isDeveloper } from '@/utils'
import AuthenticationService from '@/api/authentication.service'
import DatabaseService from '@/api/database.service'
import TableService from '@/api/table.service'
import IdentifierService from '@/api/identifier.service'

export default {
  name: 'DefaultLayout',
  components: {
  },
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
    availableLocales () {
      // return this.$i18n.locales.filter(i => i.code !== this.$i18n.locale)
      return []
    },
    token () {
      return this.$store.state.token
    },
    refreshToken () {
      return this.$store.state.refreshToken
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
    $route: {
      handler () {
        if (this.refreshToken) {
          AuthenticationService.authenticateToken(this.refreshToken)
        }
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
    if (this.refreshToken) {
      AuthenticationService.authenticateToken(this.refreshToken)
    }
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
      this.$store.commit('SET_TOKEN', null)
      this.$store.commit('SET_REFRESH_TOKEN', null)
      this.$store.commit('SET_ROLES', [])
      this.$store.commit('SET_USER', null)
      this.$store.commit('SET_ACCESS', null)
      this.$vuetify.theme.dark = false
      this.$router.push('/container')
    },
    loadDatabase () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        this.$store.commit('SET_DATABASE', null)
        return
      }
      this.loading = true
      DatabaseService.findOne(this.$route.params.container_id, this.$route.params.database_id)
        .then((database) => {
          this.$store.commit('SET_DATABASE', database)
          this.loading = false
          this.loadTable()
        })
        .catch(() => {
          this.loading = false
        })
    },
    loadTable () {
      if (!this.$route.params.container_id || !this.$route.params.database_id || !this.$route.params.table_id) {
        return
      }
      this.loading = true
      TableService.findOne(this.$route.params.container_id, this.$route.params.database_id, this.$route.params.table_id)
        .then((table) => {
          this.$store.commit('SET_TABLE', table)
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadAccess () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        return
      }
      if (!this.token) {
        return
      }
      this.loading = true
      DatabaseService.checkAccess(this.$route.params.container_id, this.$route.params.database_id)
        .then((access) => {
          this.$store.commit('SET_ACCESS', access)
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
    },
    loadIdentifier () {
      if (!this.database || 'identifier' in this.database) {
        return
      }
      this.loading = true
      IdentifierService.findOne(this.database.identifier.id)
        .then((identifier) => {
          this.database.identifier = identifier
          this.$store.commit('SET_DATABASE', this.database)
        })
        .finally(() => {
          this.loading = false
        })
    },
    retrieve () {
      this.$router.push({ path: '/search', query: { q: this.search } })
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
