import {defineStore} from 'pinia'

export const useUserStore = defineStore('user', {
  persist: true,
  state: () => {
    return {
      /** @type String */
      token: null,
      /** @type String */
      refreshToken: null,
      roles: [],
      user: null,
      access: null,
      locale: null
    }
  },
  getters: {
    getToken: (state) => state.token,
    getRefreshToken: (state) => state.refreshToken,
    getRoles: (state) => state.roles,
    getUser: (state) => state.user,
    getAccess: (state) => state.access,
    getLocale: (state) => state.locale
  },
  actions: {
    setToken(token) {
      this.token = token
    },
    setRefreshToken(refreshToken) {
      this.refreshToken = refreshToken
    },
    setRoles(roles) {
      this.roles = roles
    },
    setUser(user) {
      this.user = user
    },
    setAccess(access) {
      this.access = access
    },
    setLocale (locale) {
      this.locale = locale
    },
    logout() {
      this.token = null
      this.refreshToken = null
      this.roles = []
      this.user = null
      this.access = null
    },
    setRouteAccess(databaseId) {
      if (!databaseId) {
        return
      }
      const accessService = useAccessService()
      accessService.findOne(databaseId)
        .then(access => this.access = access)
    }
  }
})
