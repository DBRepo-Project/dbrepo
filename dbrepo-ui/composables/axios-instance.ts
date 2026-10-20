import axios, {type AxiosInstance} from 'axios'

/* refresh this many seconds before the access token expires to tolerate clock skew and request latency */
const EXPIRY_MARGIN_SECONDS = 30

let instance: AxiosInstance | null = null;
let pendingRefresh: Promise<void> | null = null;

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
    instance.interceptors.request.use(async (config) => {
      const { loggedIn, user, fetch } = useOidcAuth()
      if (!loggedIn.value) {
        return config
      }
      if (user.value!.expireAt - EXPIRY_MARGIN_SECONDS <= Date.now() / 1000) {
        /* the session endpoint refreshes the token server-side, or clears the session when it cannot; concurrent requests share one call */
        pendingRefresh ??= fetch()
          .then(() => {
            if (!loggedIn.value) {
              useToastInstance().error(useNuxtApp().$i18n.t('error.auth.expired'))
            }
          })
          .finally(() => {
            pendingRefresh = null
          })
        await pendingRefresh
      }
      const accessToken = user.value?.accessToken
      if (!accessToken) {
        return config
      }
      config.headers.Authorization = `Bearer ${accessToken}`
      return config
    })
  }
  return instance;
};
