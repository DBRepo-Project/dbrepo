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
            {{ version }} ({{ gitHash }})
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
            <v-list-item-title>{{ $t('layout.information', { name: 'vue-i18n' }) }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          to="/container"
          router>
          <v-list-item-action>
            <v-icon>mdi-database</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('layout.databases', { name: 'vue-i18n' }) }}</v-list-item-title>
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
          :type="message.type"
          v-text="message.message" />
      </div>
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
          :placeholder="$t('layout.search', { name: 'vue-i18n' })" />
        <v-btn icon class="ml-2" type="submit" name="search-submit" @click="retrieve">
          <v-icon>mdi-magnify</v-icon>
        </v-btn>
        <v-spacer />
        <div v-if="!user">
          <v-btn
            class="mr-2"
            color="secondary"
            to="/login">
            <v-icon left>mdi-login</v-icon>
            {{ $t('layout.login', { name: 'vue-i18n' }) }}
          </v-btn>
          <v-btn
            class="mr-2"
            color="primary"
            to="/signup">
            <v-icon left>mdi-account-plus</v-icon>
            {{ $t('layout.signup', { name: 'vue-i18n' }) }}
          </v-btn>
        </div>
        <div v-if="user">
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
                v-for="locale in []"
                :key="locale.code"
                :to="switchLocalePath(locale.code)">
                <v-list-item-title>{{ locale.name }}</v-list-item-title>
              </v-list-item>
              <v-list-item
                v-if="user"
                @click="logout">
                {{ $t('layout.logout', { name: 'vue-i18n' }) }}
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
  </v-app>
</template>

<script>
import AuthenticationService from '@/api/authentication.service'
import DatabaseService from '@/api/database.service'
import TableService from '@/api/table.service'

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
    availableLocales () {
      return this.$i18n.locales.filter(i => i.code !== this.$i18n.locale)
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
    gitHash () {
      return this.$config.gitHash
    },
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
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
    '$i18n.locale': {
      handler () {
        this.$store.commit('SET_LOCALE', this.$i18n.locale)
      }
    },
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
    this.$store.dispatch('reloadMessages')
    if (this.locale) {
      this.$i18n.locale = this.locale
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
        .finally(() => {
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
    retrieve () {
      this.$router.push({ path: '/search', query: { q: this.search } })
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
