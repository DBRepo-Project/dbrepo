import Vue from 'vue'
import api from '@/api'
import UserMapper from '@/api/user.mapper'

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
          const user = UserMapper.userInfoToUser(response.data)
          console.debug('response user', response.data, 'mapped user', user)
          resolve(user)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to load user', error)
          Vue.$toast.error(`[${code}] Failed to load user: ${message}`)
          reject(error)
        })
    })
  }

  updateInformation (id, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/user/${id}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const user = UserMapper.userInfoToUser(response.data)
          console.debug('response user', response.data, 'mapped user', user)
          resolve(user)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to update user information', error)
          Vue.$toast.error(`[${code}] Failed to update user information: ${message}`)
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
            Vue.$toast.error('This e-mail address is already taken')
          } else if (status === 409) {
            Vue.$toast.error('This username is already taken')
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
      api.put(`/api/user/${id}/password`, { password }, { headers: { Accept: 'application/json' } })
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
      api.put(`/api/user/${id}/theme`, { theme_dark: themeDark }, { headers: { Accept: 'application/json' } })
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
