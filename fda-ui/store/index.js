import Vue from 'vue'
import Vuex, { Store } from 'vuex'

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
    access: null
  },
  getters: {
    getToken: state => state.token,
    getRefreshToken: state => state.refreshToken,
    getRoles: state => state.roles,
    getUser: state => state.user,
    getDatabase: state => state.database,
    getTable: state => state.table,
    getAccess: state => state.access
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
    }
  },
  actions: {
  }
})
export default () => store
