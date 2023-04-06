export const state = () => ({
  token: null,
  roles: [],
  user: null,
  database: null,
  table: null,
  access: null
})

export const mutations = {
  SET_DATABASE (state, database) {
    state.database = database
  },
  SET_TOKEN (state, token) {
    state.token = token
  },
  SET_USER (state, user) {
    if (user != null && user.token) {
      delete user.token
    }
    state.user = user
  },
  SET_ROLES (state, roles) {
    state.roles = roles
  },
  SET_ACCESS (state, access) {
    state.access = access
  },
  SET_TABLE (state, table) {
    state.table = table
  }
}
