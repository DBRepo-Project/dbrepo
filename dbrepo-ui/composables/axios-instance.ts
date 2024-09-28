import axios, {type AxiosInstance} from 'axios'
import {useUserStore} from '@/stores/user'

let instance: AxiosInstance | null = null;

export const useAxiosInstance = () => {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  if (!instance) {
    instance = axios.create({
      timeout: 90_000,
      params: {},
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      baseURL: config.public.api.client
    });
    instance.interceptors.request.use((config) => {
      const token = userStore.getToken
      const refreshToken = userStore.getRefreshToken
      if (!token || !refreshToken) {
        return config
      }
      const authenticationService = useAuthenticationService()
      if (authenticationService.isExpiredToken(refreshToken)) {
        console.warn('Refresh token is expired: trigger logout of user')
        userStore.logout()
        return config
      }
      if (!authenticationService.isExpiredToken(token)) {
        config.headers.Authorization = `Bearer ${token}`
        return config
      }
      console.warn('Access token expired: request a new one')
      const userService = useUserService()
      return userService.refreshToken(refreshToken)
        .then((response: KeycloakOpenIdTokenDto) => {
          userStore.setToken(response.access_token)
          userStore.setRefreshToken(response.refresh_token)
          console.debug('new access token expires:', authenticationService.tokenToExpiryDate(response.access_token))
          config.headers.Authorization = `Bearer ${response.access_token}`
          return config
        })
        .catch((error: ApiErrorDto) => {
          if (error.code === 'error.user.credentials') {
            console.warn('User session expired.')
            userStore.logout()
          }
          return config
        });
    })
  }
  return instance;
};
