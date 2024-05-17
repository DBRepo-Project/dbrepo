import { transformAssetUrls } from 'vite-plugin-vuetify'

const proxy : any = {}

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
  process.env.NUXT_PUBLIC_API_SERVER = 'http://localhost'
}

/**
 * https://nuxt.com/docs/guide/concepts/rendering#hybrid-rendering
 */
const routeRules = {
}

export default defineNuxtConfig({
  app: {
    head: {
      charset: 'utf-8',
      viewport: 'width=device-width, initial-scale=1',
      meta: [
        { 'http-equiv': 'Content-Security-Policy', content: 'upgrade-insecure-requests' }
      ],
      htmlAttrs: {
        lang: 'en-US'
      }
    }
  },
  build: {
    transpile: ['vuetify'],
  },
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
        host: 'localhost',
        port: {
          '5672': false
        },
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
      database: {
        unsupported: '*,AVG,BIT_AND,BIT_OR,BIT_XOR,COUNT,COUNTDISTINCT,GROUP_CONCAT,JSON_ARRAYAGG,JSON_OBJECTAGG,MAX,MIN,STD,STDDEV,STDDEV_POP,STDDEV_SAMP,SUM,VARIANCE,VAR_POP,VAR_SAMP,--',
        image: {
          width: 400,
          height: 400
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
      links: {
        rabbitmq: {
          text: 'RabbitMQ Admin',
          href: '/admin/broker/'
        },
        keycloak: {
          text: 'Keycloak Admin',
          href: '/api/auth/'
        }
      },
      keycloak: {
        client: {
          id: 'dbrepo-client',
          secret: 'MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG'
        }
      }
    }
  },
  routeRules,
  devServer: {
    port: 3001
  },
  modules: [
    '@pinia/nuxt',
    '@pinia-plugin-persistedstate/nuxt',
    '@nuxtjs/i18n'
  ],
  pinia: {
    storesDirs: ['./stores/**'],
  },
  piniaPersistedstate: {
    storage: 'localStorage'
  },
  i18n: {
    lazy: true,
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
  devtools: { enabled: true }
})
