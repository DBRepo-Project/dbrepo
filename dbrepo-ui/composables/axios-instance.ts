import axios, {type AxiosInstance} from 'axios'

let instance: AxiosInstance | null = null;

export const useAxiosInstance = () => {
  const config = useRuntimeConfig()
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
      const { loggedIn, user } = useOidcAuth()
      if (!loggedIn) {
        return config
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
