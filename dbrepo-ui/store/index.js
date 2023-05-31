import Vue from 'vue'
import Vuex, { Store } from 'vuex'
import UserService from '@/api/user.service'
import DatabaseService from '@/api/database.service'
import TableService from '@/api/table.service'
import MetadataService from '@/api/metadata.service'
import SemanticService from '@/api/semantic.service'

Vue.use(Vuex)

// https://github.com/hua1995116/webchat/blob/7c6544d3defd41cb7cf68306accea97800858bc3/client/src/store/index.js#L293
const store = new Store({
  state: {
    token: null,
    refreshToken: null,
    roles: [],
    user: null,
    database: null,
    table: null,
    access: null,
    locale: null,
    messages: [],
    ontologies: []
  },
  getters: {
    getToken: state => state.token,
    getRefreshToken: state => state.refreshToken,
    getRoles: state => state.roles,
    getUser: state => state.user,
    getDatabase: state => state.database,
    getTable: state => state.table,
    getAccess: state => state.access,
    getLocale: state => state.locale,
    getMessages: state => state.messages,
    getOntologies: state => state.ontologies
  },
  mutations: {
    SET_TOKEN (state, token) {
      state.token = token
    },
    SET_REFRESH_TOKEN (state, refreshToken) {
      state.refreshToken = refreshToken
    },
    SET_ROLES (state, roles) {
      state.roles = roles
    },
    SET_USER (state, user) {
      state.user = user
    },
    SET_DATABASE (state, database) {
      state.database = database
    },
    SET_TABLE (state, table) {
      state.table = table
    },
    SET_ACCESS (state, access) {
      state.access = access
    },
    SET_LOCALE (state, locale) {
      state.locale = locale
    },
    SET_MESSAGES (state, messages) {
      state.messages = messages
    },
    SET_ONTOLOGIES (state, ontologies) {
      state.ontologies = ontologies
    }
  },
  actions: {
    reloadUser ({ state, commit }) {
      UserService.findOne(state.user.id)
        .then((user) => {
          commit('SET_USER', user)
        })
    },
    reloadAccess ({ state, commit }) {
      DatabaseService.checkAccess(state.container.id, state.database.id)
        .then((access) => {
          commit('SET_ACCESS', access)
        })
    },
    reloadDatabase ({ state, commit }) {
      DatabaseService.findOne(state.database.container.id, state.database.id)
        .then((database) => {
          commit('SET_DATABASE', database)
        })
    },
    reloadTable ({ state, commit }) {
      TableService.findOne(state.database.container.id, state.database.id, state.table.id)
        .then((table) => {
          commit('SET_TABLE', table)
        })
    },
    reloadMessages ({ state, commit }) {
      MetadataService.findActiveMessages()
        .then((messages) => {
          commit('SET_MESSAGES', messages)
        })
    },
    reloadOntologies ({ state, commit }) {
      SemanticService.findAllOntologies()
        .then((ontologies) => {
          commit('SET_ONTOLOGIES', ontologies)
        })
    }
  }
})
export default () => store
