import axios, {AxiosError, type AxiosInstance} from 'axios'
import {useUserStore} from '@/stores/user'

let instance: AxiosInstance | null = null;

export const useAxiosInstance = () => {
  const config = useRuntimeConfig();
  const userStore = useUserStore()
  if (!instance) {
    instance = axios.create({
      timeout: 3000,
      params: {},
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      baseURL: config.public.api.client
    });
    instance.interceptors.request.use((config) => new Promise((resolve, reject) => {
      const token = userStore.getToken
      const refreshToken = userStore.getRefreshToken
      if (!token || !refreshToken) {
        resolve(config);
        return
      }
      const authenticationService = useAuthenticationService()
      if (authenticationService.isExpiredToken(refreshToken)) {
        console.warn('Refresh token is expired: trigger logout of user')
        userStore.logout()
        resolve(config);
        return
      }
      if (!authenticationService.isExpiredToken(token)) {
        config.headers.Authorization = `Bearer ${token}`
        resolve(config);
        return
      }
      console.warn('Access token expired: request a new one')
      authenticationService.authenticateToken(refreshToken)
        .then((response: KeycloakOpenIdTokenDto) => {
          userStore.setToken(response.access_token)
          userStore.setRefreshToken(response.refresh_token)
          console.debug('new access token expires:', authenticationService.tokenToExpiryDate(response.access_token))
          config.headers.Authorization = `Bearer ${response.access_token}`
          resolve(config);
          return
        })
        .error((error: AxiosError) => {
          if (parseKeycloakError(error)?.error == 'invalid_grant') {
            console.error('Invalid user credentials: perform logout')
            userStore.logout()
          }
          reject(error);
          return
        });
      /* should never happen */
      resolve(config);
      return
    }))
  }
  return instance;
};

function parseKeycloakError(error: AxiosError): KeycloakErrorDto | null {
  if (!error || !error.response || !error.response.data) {
    return null
  }
  return (error.response.data as KeycloakErrorDto)
}
