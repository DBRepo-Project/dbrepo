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
          to="/database"
          router
          exact>
          <v-list-item-action>
            <v-icon>mdi-database</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('layout.databases', { name: 'vue-i18n' }) }}</v-list-item-title>
          </v-list-item-content>
          <v-list-item-action v-if="databaseCount" v-text="databaseCount.all" />
        </v-list-item>
        <v-list-item
          v-if="user"
          to="/database?f=my"
          router
          exact>
          <v-list-item-action>
            <v-icon>mdi-bookmark-outline</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t('layout.mydatabases', { name: 'vue-i18n' }) }}</v-list-item-title>
          </v-list-item-content>
          <v-list-item-action v-if="databaseCount" v-text="databaseCount.my" />
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
          append-icon="mdi-magnify"
          :placeholder="$t('layout.search', { name: 'vue-i18n' })"
          @click:append="retrieve" />
        <v-btn class="ml-2" plain type="submit" name="search-advanced" @click="toggleAdvancedSearch">
          Advanced
        </v-btn>
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
            <!--
            <v-list-item
              v-for="locale in []"
              :key="locale.code"
              :to="switchLocalePath(locale.code)">
              <v-list-item-title>{{ locale.name }}</v-list-item-title>
            </v-list-item>
            -->
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
      <!-- Advanced Search card -->
      <v-card v-if="showAdvancedSearch" id="advanced_search" flat tile>
        <v-card-text>
          <v-container fluid>
            <v-row>
              <v-col cols="auto">
                <v-select
                  v-model="advancedSearchData.type"
                  :items="fieldItems"
                  item-text="name"
                  item-value="value"
                  label="Type" />
              </v-col>
              <v-col cols="auto">
                <v-text-field v-model="advancedSearchData.id" clearable label="ID" variant="underlined" />
              </v-col>
              <v-col cols="auto">
                <v-text-field
                  v-if="!hideFields.hideNameField"
                  v-model="advancedSearchData.name"
                  clearable
                  label="Name"
                  variant="underlined" />
              </v-col>
              <v-col cols="auto">
                <v-text-field
                  v-if="!hideFields.hideInternalNameField"
                  v-model="advancedSearchData.internal_name"
                  clearable
                  label="Internal Name"
                  variant="underlined" />
              </v-col>
            </v-row>
            <v-row v-if="fieldsResponse">
              <!-- Loop through fields of Response -->
              <span v-for="field in fieldsResponse.fields" :key="`${field.attribute_name}`">
                <!-- Loop through "fields" list -->
                <template v-if="shouldRenderItem(field)">
                  <v-col cols="auto">
                    <v-select
                      v-if="field.type === 'boolean'"
                      v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                      clearable
                      :items="booleanItems"
                      item-text="name"
                      item-value="value"
                      :label="generateFriendlyName(field)" />
                    <v-text-field
                      v-if="(field.type === 'keyword' && field.attribute_name !== 'column_type') || field.type === 'text' || field.type === 'date'"
                      v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                      type="text"
                      :label="generateFriendlyName(field)"
                      clearable />
                    <v-select
                      v-if="field.type === 'keyword' && field.attribute_name === 'column_type'"
                      v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                      :items="columnTypes"
                      item-value="value"
                      clearable
                      :label="generateFriendlyName(field)" />
                    <v-text-field
                      v-if="field.type === 'integer'"
                      v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                      type="number"
                      :label="generateFriendlyName(field)"
                      clearable />
                  </v-col>
                </template>
              </span>
            </v-row>
          </v-container>
        </v-card-text>
        <v-card-text>
          <v-btn class="mr-2" color="primary" small @click="advancedSearch">
            Search
          </v-btn>
          <v-btn small @click="toggleAdvancedSearch">
            Cancel
          </v-btn>
        </v-card-text>
      </v-card>
      <v-container>
        <nuxt />
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import SearchService from '@/api/search.service'
import AuthenticationService from '@/api/authentication.service'
import DatabaseService from '@/api/database.service'
import EventBus from '@/api/eventBus'
import TableService from '@/api/table.service'
import QueryMapper from '@/api/query.mapper'

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
      showAdvancedSearch: false,
      columnTypes: QueryMapper.mySql8DataTypes().map((datatype) => {
        datatype.value = datatype.value.toUpperCase()
        return datatype
      }),
      fieldItems: [
        { name: 'Database', value: 'database' },
        { name: 'Table', value: 'table' },
        { name: 'Column', value: 'column' },
        { name: 'User', value: 'user' },
        { name: 'Identifier', value: 'identifier' },
        { name: 'Concept', value: 'concept' },
        { name: 'Unit', value: 'unit' },
        { name: 'View', value: 'view' }
      ],
      booleanItems: [
        { name: 'True', value: true },
        { name: 'False', value: false }
      ],
      fieldsResponse: null,
      advancedSearchData: {
        name: null,
        internal_name: null,
        id: null,
        type: null
      }
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
    databaseCount () {
      return this.$store.state.databaseCount
    },
    hideFields () {
      const selectedOption = this.advancedSearchData.type
      return {
        hideNameField: selectedOption === 'identifier',
        hideInternalNameField: ['identifier', 'user', 'concept', 'unit'].includes(selectedOption)
      }
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
          this.$store.dispatch('reloadDatabaseCount')
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
    },
    user: {
      handler () {
        if (!this.user) {
          return
        }
        DatabaseService.countAll(true)
          .then((count) => {
            this.databaseCountFilter = count
          })
      }
    },
    'advancedSearchData.type': {
      handler (newType, oldType) {
        if (!newType) {
          return
        }
        console.debug('switched advanced search type to', newType)
        this.resetAdvancedSearchFields()
        SearchService.getFields(newType)
          .then((response) => {
            this.fieldsResponse = response
          })
      },
      immediate: true
    }
  },
  mounted () {
    this.initEnvironment()
    this.$store.dispatch('reloadDatabaseCount')
    this.$store.dispatch('reloadMessages')
    this.$store.dispatch('reloadOntologies')
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
    /* Removes all advanced search fields when switching the type */
    resetAdvancedSearchFields () {
      Object.keys(this.advancedSearchData)
        .filter(k => !['name', 'internal_name', 'id', 'type'].includes(k))
        .forEach(k => delete this.advancedSearchData[k])
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
      if (!this.token) {
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
    },
    advancedSearch () {
      console.debug('performing advanced search')
      if (this.search) {
        this.advancedSearchData.search_term = this.search
      } else {
        delete this.advancedSearchData.search_term
      }
      EventBus.$emit('advancedSearchButtonClicked')
      this.$router.push({ path: '/search' })
    },
    toggleAdvancedSearch () {
      this.showAdvancedSearch = !this.showAdvancedSearch
    },
    isAdvancedSearchEmpty () {
      return !(
        this.advancedSearchData.type ||
        this.advancedSearchData.id ||
        this.advancedSearchData.name ||
        this.advancedSearchData.internal_name
      )
    },
    dynamicFieldsMap () {
      // Defines a mapping to narrow down the fields rendered for the advanced search
      return {
        database: ['created', 'description', 'is_public'],
        table: ['created', 'description', 'is_public'],
        column: ['column_type', 'is_primary_key', 'is_null_allowed'],
        user: ['firstname', 'lastname', 'username'],
        identifier: [
          'creators.properties.creator_name', 'creators.properties.name_identifier',
          'descriptions.properties.description', 'doi', 'funders.properties.funder_identifier',
          'licenses', 'publication_year', 'titles.properties.title', 'visibility'
        ],
        view: ['is_public', 'query'],
        concept: ['uri'],
        unit: ['uri']
      }
    },
    getLastFlattenedItem (str) {
      // Returns substring after the last dot otherwise the string itself if no dots are contained
      if (!str) { return '' }

      // Check if string is a flattened nested object
      return str.includes('.') ? str.split('.').slice(-1)[0] : str
    },
    generateFriendlyName (item) {
      // Generates a proper name to be displayed with the dynamic component
      if (!item) { return '' }

      const specialAbbreviations = {
        doi: 'DOI',
        uri: 'URI'
        // Add more abbreviations here, if needed
      }
      const str = this.getLastFlattenedItem(item.attribute_name)

      return str.split('_').map((word) => {
        const lowerWord = word.toLowerCase()
        return specialAbbreviations[lowerWord] || (word.charAt(0).toUpperCase() + word.slice(1))
      }).join(' ')
    },
    generateDynamicVModelKey (item) {
      // Generates a dynamic v-model; It will be attached to the advancedSearchData object
      if (!item) { return '' }

      return `${this.advancedSearchData.type}.${item.attribute_name}`
    },
    shouldRenderItem (item) {
      // Checks if item's attribute_name matches any wanted field
      // The expected response is of a flattened format, so this method must be modified accordingly if the response is changed
      return this.dynamicFieldsMap()[this.advancedSearchData.type].includes(item.attribute_name)
    }
  },
  head () {
    return {
      title: this.$config.title
    }
  },
  provide () {
    return {
      advancedSearchData: this.advancedSearchData
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
