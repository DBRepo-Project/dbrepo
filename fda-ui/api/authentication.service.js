import Vue from 'vue'
import qs from 'qs'
import api from '@/api/api'
import { api as endpoint, clientSecret } from '@/config'

class AuthenticationService {
  authenticate (username, password) {
    const payload = {
      client_id: 'dbrepo-client',
      username,
      password,
      grant_type: 'password',
      client_secret: clientSecret,
      scope: 'openid profile roles'
    }
    api.post(`${endpoint.replace('http:', 'https:')}/api/auth/realms/dbrepo/protocol/openid-connect/token`, qs.stringify(payload), {
      headers: { ContentType: 'application/form-data' }
    }).then((response) => {
      console.info('====>', response)
    }).catch((error) => {
      console.error('Failed to authenticate', error)
      const { code, message } = error
      Vue.$toast.error(`[${code}] Failed to authenticate: ${message}`)
    })
  }
}

export default new AuthenticationService()
