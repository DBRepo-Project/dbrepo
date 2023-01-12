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
      <v-autocomplete
        v-model="model"
        :items="searchResults"
        :loading="loadingSearch"
        :search-input.sync="query"
        hide-no-data
        hide-selected
        hide-details
        item-text="name"
        item-value="name"
        solo
        flat
        single-line
        label="Search ..."
        return-object>
        <template v-slot:item="data">
          <template>
            <v-list-item-icon>
              <v-icon :title="metadata(data).text">{{ metadata(data).icon }}</v-icon>
            </v-list-item-icon>
            <v-list-item-content @click="navigate(data)">
              <v-list-item-title class="search-result-title">{{ metadata(data).title }}</v-list-item-title>
              <v-list-item-subtitle class="search-result-subtitle">{{ metadata(data).subtitle }}</v-list-item-subtitle>
            </v-list-item-content>
          </template>
        </template>
      </v-autocomplete>
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
    db () {
      return this.$store.state.db
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
    $route () {
      this.loadDB()
      if (this.user) {
        this.setTheme()
      }
    },
    query (val) {
      if (!val) {
        return
      }
      setTimeout(() => {
        if (val !== this.query) {
          return
        }
        this.queryDatabases(val)
        this.queryTables(val)
        this.queryColumns(val)
      })
    }
  },
  mounted () {
    this.loadDB()
    this.loadUser()
    this.loadContainers()
      .then(() => this.loadDatabases())
    this.setTheme()
  },
  methods: {
    metadata (item) {
      if (item.item.exchange !== undefined) {
        /* is database */
        return {
          icon: 'mdi-database',
          text: 'Database',
          link: `/container/${item.item.id}/database/${item.item.id}`,
          title: item.item.name,
          subtitle: item.item.description
        }
      }
      if (item.item.topic !== undefined) {
        /* is table */
        return {
          icon: 'mdi-table',
          text: 'Table',
          link: `/container/${item.item.tdbid}/database/${item.item.tdbid}/table/${item.item.id}`,
          title: item.item.name,
          subtitle: item.item.description
        }
      }
      if (item.item.columnType !== undefined) {
        /* is column */
        return {
          icon: 'mdi-view-column-outline',
          text: 'Column',
          link: `/container/${item.item.cdbid}/database/${item.item.cdbid}/table/${item.item.tid}`,
          title: item.item.name,
          subtitle: item.item.columnType
        }
      }
      /* is identifier */
      return {
        icon: 'mdi-lock-clock',
        text: 'Identifier',
        link: `/pid/${item.item.id}`,
        title: item.item.name,
        subtitle: item.item.description
      }
    },
    login () {
      const redirect = ![undefined, '/', '/login'].includes(this.$router.currentRoute.path)
      this.$router.push({ path: '/login', query: redirect ? { redirect: this.$router.currentRoute.path } : {} })
    },
    navigate (item) {
      this.$router.push(this.metadata(item).link)
    },
    logout () {
      this.$store.commit('SET_TOKEN', null)
      this.$store.commit('SET_USER', null)
      this.$vuetify.theme.dark = false
      this.$router.push('/container')
    },
    async queryDatabases (v) {
      this.loadingSearch = true
      try {
        const res = await this.$axios.get(`/search/databaseindex,tableindex,columnindex/_search?q=${v}*&terminate_after=50`)
        console.info('search results', res.data.hits.total.value)
        console.debug('search results', res.data.hits.hits)
        this.searchResults = res.data.hits.hits.map(h => h._source)
      } catch (err) {
        console.error('Failed to load search results', err)
      }
      this.loadingSearch = false
    },
    async loadContainers () {
      this.createDbDialog = false
      try {
        this.loadingContainers = true
        const res = await this.$axios.get('/api/container/')
        this.containers = res.data
        console.debug('containers', this.containers)
        this.error = false
      } catch (err) {
        console.error('containers', err)
        this.error = true
      }
      this.loadingContainers = false
    },
    async loadDatabases () {
      if (this.containers.length === 0) {
        return
      }
      this.loading = true
      for (const container of this.containers) {
        try {
          const res = await this.$axios.get(`/api/container/${container.id}/database`, this.config)
          for (const info of res.data) {
            info.container_id = container.id
            this.databases.push(info)
          }
        } catch (err) {
          if (err.response === undefined || err.response.status === undefined || err.response.status !== 401) {
            console.error('Failed to load databases for container', err)
          }
        }
      }
      this.loading = false
      console.debug('databases', this.databases)
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      try {
        this.loadingUser = true
        const res = await this.$axios.put('/api/auth', {}, this.config)
        this.$store.commit('SET_USER', res.data)
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
