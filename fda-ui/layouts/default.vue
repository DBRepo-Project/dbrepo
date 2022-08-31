<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" fixed app>
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
          class="tu-logo"
          src="/tu_logo_512.png" />
        <v-img
          contain
          class="univie-logo"
          src="/univie_logo_512.png" />
      </div>
    </v-navigation-drawer>
    <v-app-bar fixed app>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      <v-spacer />
      <v-btn
        v-if="!token"
        class="mr-2"
        color="secondary"
        to="/login">
        <v-icon left>mdi-login</v-icon> Login
      </v-btn>
      <v-btn
        v-if="!token"
        class="mr-2"
        color="primary"
        to="/signup">
        <v-icon left>mdi-account-plus</v-icon> Signup
      </v-btn>
      <v-btn v-if="username" to="/user" plain>
        {{ username }} <sup v-if="user.email_verified">
          <v-icon color="primary" title="E-Mail verified" small>mdi-check-decagram</v-icon>
        </sup>
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
        class="error text-center">
        <v-card-text class="black--text">
          This is a <strong>TEST</strong> environment, do not use production/confidential data! — <a href="//github.com/fair-data-austria/dbrepo/issues/new" class="black--text">Report a bug</a>
        </v-card-text>
      </v-card>
    </v-footer>
  </v-app>
</template>

<script>
export default {
  name: 'DefaultLayout',
  data () {
    return {
      drawer: false,
      user: {
        theme_dark: null
      },
      loadingUser: true
    }
  },
  computed: {
    availableLocales () {
      return this.$i18n.locales.filter(i => i.code !== this.$i18n.locale)
    },
    token () {
      return this.$store.state.token
    },
    username () {
      return this.$store.state.user && this.$store.state.user.username
    },
    container () {
      return this.$store.state.container
    },
    filteredItems () {
      return this.items.filter((x) => {
        if (x.needsContainer && !this.container) { return false }
        return true
      })
    },
    db () {
      return this.$store.state.db
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
    sandbox () {
      if (this.$config.sandbox === undefined) {
        console.debug('env sandbox not found, default to', false)
        return false
      }
      console.debug('env sandbox found', this.$config.sandbox)
      return this.$config.sandbox
    }
  },
  watch: {
    $route () {
      this.loadDB()
      if (this.token) {
        this.loadUser()
          .then(() => this.setTheme())
      }
    }
  },
  mounted () {
    this.loadDB()
    this.loadUser()
      .then(() => this.setTheme())
  },
  methods: {
    logout () {
      this.$store.commit('SET_TOKEN', null)
      this.$store.commit('SET_USER', null)
      this.$toast.success('Logged out')
      this.$vuetify.theme.dark = false
      this.$router.push('/container')
    },
    async loadDB () {
      if (this.$route.params.db_id && !this.db) {
        try {
          const res = await this.$axios.get(`/api/database/${this.$route.params.db_id}`)
          this.$store.commit('SET_DATABASE', res.data)
        } catch (err) {
          console.error('Failed to load database', err)
        }
      }
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      try {
        this.loadingUser = true
        const res = await this.$axios.put('/api/auth', {}, this.config)
        console.debug('user data', res.data)
        this.user = res.data
      } catch (err) {
        const { status } = err.response
        if (status === 401) {
          console.error('Token expired', err)
          this.logout()
        } else {
          console.error('user data', err)
          this.$toast.error('Failed to load user')
          this.error = true
        }
      }
      this.loadingUser = false
    },
    setTheme () {
      this.$vuetify.theme.dark = this.user.theme_dark
    }
  }
}
</script>
<style scoped>
.tu-logo {
  margin: 1em 1em 0;
}
.univie-logo {
  margin: 1em 1em .5em;
}
.sl {
  padding-left: 36px;
}
</style>
