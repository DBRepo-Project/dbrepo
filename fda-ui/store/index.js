export const state = () => ({
  token: null,
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
  SET_ACCESS (state, access) {
    state.access = access
  },
  /**
   Workaround. Helps to go 'back' from table data view and
   have the accordion open on the same table
   */
  SET_TABLE (state, table) {
    state.table = table
  }
}
