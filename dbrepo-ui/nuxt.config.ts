import vuetify, {transformAssetUrls} from 'vite-plugin-vuetify'

const proxy: any = {}
const publicAuthBase = (process.env.BASE_URL || 'http://localhost').replace(/\/$/, '')
const internalAuthBase = (process.env.AUTH_SERVICE_ENDPOINT || (process.env.NODE_ENV === 'development'
  ? 'http://localhost:8080'
  : 'http://auth-service:8080')).replace(/\/$/, '')

/* proxies the backend calls, >>NOT<< the frontend calls */
if (process.env.NODE_ENV === 'development') {
  const api = 'http://localhost'
  proxy['/api'] = api
  proxy['/pid'] = {
    target: api + '/api',
    changeOrigin: true,
    pathRewrite: {
      '^/pid': '/pid'
    }
  }
  process.env.VERSION = 'bun-dev'
  process.env.NUXT_PUBLIC_API_SERVER = api
  process.env.NUXT_STORAGE_OIDC_HOST = 'localhost'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_BASE_URL = api + '/realms/dbrepo'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_AUTHORIZATION_URL = api + '/realms/dbrepo/protocol/openid-connect/auth'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_CLIENT_ID = 'dbrepo-client'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_CLIENT_SECRET = 'MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_REDIRECT_URI = api + ':3001/auth/keycloak/callback'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_TOKEN_URL = internalAuthBase + '/realms/dbrepo/protocol/openid-connect/token'
  process.env.NUXT_OIDC_PROVIDERS_KEYCLOAK_USER_INFO_URL = internalAuthBase + '/realms/dbrepo/protocol/openid-connect/userinfo'
}

/**
 * https://nuxt.com/docs/guide/concepts/rendering#hybrid-rendering
 */
export default defineNuxtConfig({
  app: {
    head: {
      charset: 'utf-8',
      viewport: 'width=device-width, initial-scale=1',
      meta: [
        {'http-equiv': 'Content-Security-Policy', content: 'upgrade-insecure-requests'}
      ],
      htmlAttrs: {
        lang: 'en-US'
      }
    }
  },

  logLevel: 'verbose',

  build: {
    transpile: ['vuetify'],
  },

  builder: 'vite',

  css: [
    'vuetify/lib/styles/main.sass',
    '@mdi/font/css/materialdesignicons.min.css',
    '@/assets/globals.css',
    '@/assets/overrides.css',
  ],

  runtimeConfig: {
    public: {
      commit: '',
      title: 'Database Repository',
      logo: '/logo.svg',
      icon: '/favicon.ico',
      touch: '/apple-touch-icon.png',
      version: 'bun-dev',
      broker: {
        /* mark encrypted connection with leading ^, e.g. ^amqp://localhost:5671/dbrepo will be displayed with (encrypted) suffix */
        connections: "amqp://localhost:5672/dbrepo,mqtt://localhost:1883/dbrepo",
        extra: ''
      },
      variant: {
        input: {
          normal: 'underlined',
          contrast: 'outlined',
        },
        button: {
          normal: 'flat',
          contrast: 'outlined',
        },
        list: {
          normal: '',
          contrast: 'flat',
        }
      },
      dashboard: {
        url: 'http://localhost/dashboard'
      },
      api: {
        client: 'http://localhost',
        server: 'http://gateway-service',
      },
      database: {
        image: {
          width: 200,
          height: 200
        },
        extra: ''
      },
      pid: {
        default: {
          publisher: 'Example University'
        }
      },
      about: {
        content: ''
      },
      policies: {
        content: ''
      },
      terms: {
        content: ''
      },
      doi: {
        endpoint: 'https://doi.org'
      },
      links: {}
    },
    storage: {
      oidc: {
        host: 'cache-db-primary',
        port: 6379,
        tls: false,
        base: 'oidc',
        username: 'default',
        password: 'valkey',
      }
    },
  },

  devServer: {
    port: 3001
  },

  nitro: {
    preset: 'node-server'
    /* rest is defined in modules/storage/runtime/plugin.ts as Nitro plugin to enable env-var configuration */
  },

  oidc: {
    enabled: true,
    defaultProvider: 'keycloak',
    middleware: {
      globalMiddlewareEnabled: false,
      customLoginPage: false
    },
    session: {
      automaticRefresh: true,
      expirationCheck: true
    },
    providers: {
      keycloak: {
        audience: 'account',
        baseUrl: `${publicAuthBase}/realms/dbrepo`,
        clientId: 'dbrepo-client',
        clientSecret: 'MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG',
        redirectUri: `${publicAuthBase}/auth/keycloak/callback`,
        logoutRedirectUri: publicAuthBase,
        authorizationUrl: `${publicAuthBase}/realms/dbrepo/protocol/openid-connect/auth`,
        tokenUrl: `${internalAuthBase}/realms/dbrepo/protocol/openid-connect/token`,
        userInfoUrl: `${internalAuthBase}/realms/dbrepo/protocol/openid-connect/userinfo`,
        logoutUrl: `${publicAuthBase}/realms/dbrepo/protocol/openid-connect/logout`,
        openIdConfiguration: {
          issuer: `${publicAuthBase}/realms/dbrepo`,
          jwks_uri: `${internalAuthBase}/realms/dbrepo/protocol/openid-connect/certs`,
        },
        exposeAccessToken: true,
        optionalClaims: ['realm_access'],
      },
    },
  },

  modules: [
    ['@artmizu/nuxt-prometheus', {verbose: false}],
    '@nuxtjs/i18n',
    '@pinia/nuxt',
    '@pinia-plugin-persistedstate/nuxt',
    'nuxt-oidc-auth',
    async (options, nuxt) => {
      nuxt.hooks.hook('vite:extendConfig', config => config.plugins.push(
        vuetify()
      ))
    },
  ],

  pinia: {
    storesDirs: ['./stores/**'],
  },

  piniaPersistedstate: {
    storage: 'localStorage'
  },

  i18n: {
    lazy: false,
    langDir: 'locales',
    strategy: 'no_prefix',
    defaultLocale: 'de',
    locales: [
      {
        'code': 'en',
        'file': 'en-US.json',
        'name': 'English (US)',
        'language': 'en-US'
      }
    ]

  },

  vite: {
    server: {
      proxy
    },
    vue: {
      template: {
        transformAssetUrls,
      },
    },
  },

  devtools: {
    enabled: false
  },

  compatibilityDate: '2025-09-04'
})
