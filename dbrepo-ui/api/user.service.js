import api, { displayError } from '@/api'
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
          displayError(error, 'Failed to load users')
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
          displayError(error, 'Failed to load user')
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
          displayError(error, 'Failed to update user information')
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
          const { status } = error
          if (status === 417) {
            displayError(error, 'This e-mail address is already taken')
          } else if (status === 409) {
            displayError(error, 'This username is already taken')
          } else if (status === 428) {
            displayError(error, 'Account was created')
          } else {
            displayError(error, 'Failed to create user')
          }
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
          displayError(error, 'Failed to update password')
          reject(error)
        })
    })
  }

  updateTheme (id, themeDark) {
    return new Promise((resolve, reject) => {
      api.put(`/api/user/${id}/theme`, { theme_dark: themeDark }, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError(error, 'Failed to update theme')
          reject(error)
        })
    })
  }
}

export default new UserService()
