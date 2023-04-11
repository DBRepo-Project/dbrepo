import Vue from 'vue'
import api from '@/api'

class UserService {
  findAll () {
    return new Promise((resolve, reject) => {
      api.get('/api/user', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const users = response.data
          console.debug('response users', users)
          resolve(users)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load users', error)
          Vue.$toast.error(`[${code}] Failed to load users: ${message}`)
          reject(error)
        })
    })
  }

  findOne (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/user/${id}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const user = response.data
          console.debug('response user', user)
          resolve(user)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to load user', error)
          Vue.$toast.error(`[${code}] Failed to load user: ${message}`)
          reject(error)
        })
    })
  }

  create (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/user', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const user = response.data
          console.debug('response user', user)
          resolve(user)
        }).catch((error) => {
          const { code, message, response } = error
          const { status } = response
          if (status === 417) {
            Vue.$toast.error(`[${code}] This e-mail address is taken: ${message}`)
          } else if (status === 409) {
            Vue.$toast.error(`[${code}] This username is taken: ${message}`)
          } else if (status === 428) {
            Vue.$toast.warning(`[${code}] Account was created: ${message}`)
          } else {
            Vue.$toast.error(`[${code}] Failed to create user: ${message}`)
          }
          console.error('Failed to create user', error)
          this.loading = false
          reject(error)
        })
    })
  }

  updatePassword (id, password) {
    return new Promise((resolve, reject) => {
      api.post(`/api/user/${id}/password`, { password }, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to update user password', error)
          Vue.$toast.error(`[${code}] Failed to update user password: ${message}`)
          reject(error)
        })
    })
  }

  updateTheme (id, themeDark) {
    return new Promise((resolve, reject) => {
      api.post(`/api/user/${id}/theme`, { theme_dark: themeDark }, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to update user theme', error)
          Vue.$toast.error(`[${code}] Failed to update user theme: ${message}`)
          reject(error)
        })
    })
  }
}

export default new UserService()
