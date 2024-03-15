import axios from 'axios'
import qs from 'qs'
import {jwtDecode} from 'jwt-decode'

export const useAuthenticationService = (): any => {

  function authenticatePlain(username: string, password: string): Promise<KeycloakOpenIdTokenDto> {
    const config = useRuntimeConfig()
    const payload = {
      client_id: config.public.keycloak.client.id,
      client_secret: config.public.keycloak.client.secret,
      username,
      password,
      grant_type: 'password',
      scope: 'roles'
    }
    if (!username) {
      new Error('parameter username is empty')
    }
    if (!password) {
      new Error('parameter password is empty')
    }
    if (!payload.client_secret) {
      new Error('parameter clientSecret is empty')
    }
    return _authenticate(payload)
  }

  function authenticateToken(refreshToken: string): Promise<KeycloakOpenIdTokenDto> {
    const config = useRuntimeConfig()
    const payload = {
      client_id: config.public.keycloak.client.id,
      client_secret: config.public.keycloak.client.secret,
      grant_type: 'refresh_token',
      refresh_token: refreshToken
    }
    if (!refreshToken) {
      new Error('parameter refreshToken is empty')
    }
    if (!payload.client_secret) {
      new Error('parameter clientSecret is empty')
    }
    return _authenticate(payload)
  }

  /**
   * Authenticate method. This method *needs* its own axios instance, infinite dependency loop otherwise!
   * @param payload
   */
  function _authenticate(payload: any): Promise<KeycloakOpenIdTokenDto> {
    const config = useRuntimeConfig();
    console.debug('obtain tokens')
    return new Promise<KeycloakOpenIdTokenDto>((resolve, reject) => {
      const instance = axios.create({
        timeout: 3000,
        params: {},
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        baseURL: config.public.api.client
      });
      instance.post<KeycloakOpenIdTokenDto>('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload))
        .then((response) => {
          const userStore = useUserStore()
          const userService = useUserService()
          // eslint-disable-next-line camelcase
          const {access_token, refresh_token} = response.data
          userStore.setToken(access_token)
          userStore.setRefreshToken(refresh_token)
          userStore.setRoles(userService.tokenToRoles(access_token))
          console.info('Obtained tokens')
          resolve(response.data);
        })
        .catch((error: KeycloakErrorDto) => {
          reject(error);
        })
    })
  }

  function isExpiredToken(token: string): boolean {
    if (!token) {
      return false
    }
    return tokenToExpiryDate(token) < Date.now()
  }

  function tokenToExpiryDate(token: string): number {
    if (!token) {
      return -1
    }
    const exp: number = jwtDecode<Token>(token).exp
    if (exp) {
      return exp * 1000
    }
    return -1
  }

  return {authenticatePlain, authenticateToken, isExpiredToken, tokenToExpiryDate}
}
