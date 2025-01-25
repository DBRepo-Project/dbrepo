import {transformAssetUrls} from 'vite-plugin-vuetify'
import vuetify from 'vite-plugin-vuetify'

const proxy: any = {}

/* proxies the backend calls, >>NOT<< the frontend calls (clicking) */
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
  process.env.NUXT_PUBLIC_API_SERVER = api
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
      api: {
        client: 'http://localhost',
        server: 'http://gateway-service',
      },
      upload: {
        client: 'http://localhost/api/upload/files',
        prefix: '/'
      },
      database: {
        unsupported: 'AVG,BIT_AND,BIT_OR,BIT_XOR,COUNT,COUNTDISTINCT,GROUP_CONCAT,JSON_ARRAYAGG,JSON_OBJECTAGG,MAX,MIN,STD,STDDEV,STDDEV_POP,STDDEV_SAMP,SUM,VARIANCE,VAR_POP,VAR_SAMP,--',
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
      doi: {
        enabled: false,
        endpoint: 'https://doi.org'
      },
      links: {}
    }
  },

  devServer: {
    port: 3001
  },

  oidc: {
    providers: {
      keycloak: {
        audience: 'account',
        baseUrl: 'http://localhost:8080/realms/dbrepo',
        clientId: 'dbrepo-client',
        clientSecret: '', // inject on runtime
        scope: ['openid', 'roles'],
        optionalClaims: ['realm_access'],
        redirectUri: 'http://localhost/auth/keycloak/callback',
        userNameClaim: 'preferred_username',
        exposeAccessToken: true,
        logoutRedirectUri: 'http://localhost',
      },
    },
    middleware: {
      globalMiddlewareEnabled: false
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
        'iso': 'en-US'
      },
      {
        'code': 'de',
        'file': 'de-AT.json',
        'name': 'German (AT)',
        'iso': 'de-AT'
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
  compatibilityDate: '2025-01-25'
})
