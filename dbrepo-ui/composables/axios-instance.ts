import axios, {type AxiosInstance} from 'axios'

let instance: AxiosInstance | null = null;

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

function isExpiredToken(token: string): boolean {
  if (!token) {
    return false
  }
  return tokenToExpiryDate(token) < Date.now()
}

export const useAxiosInstance = () => {
  const config = useRuntimeConfig()
  if (!instance) {
    instance = axios.create({
      timeout: 300_000,
      params: {},
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      baseURL: config.public.api.client
    });
    instance.interceptors.request.use(async (config) => {
      const { loggedIn, user, canRefresh, refresh } = useOidcAuth()
      if (!loggedIn) {
        return config
      }
      if (canRefresh) {
        await refresh()
      }
      const { accessToken } = user.value
      if (!accessToken) {
        return config
      }
      config.headers.Authorization = `Bearer ${accessToken}`
      return config
    })
  }
  return instance;
};
