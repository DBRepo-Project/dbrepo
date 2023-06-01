import path from 'path'
import colors from 'vuetify/es5/util/colors'
import { api, icon, search, clientSecret, title, sandbox, logo, version, defaultPublisher, doiUrl, baseUrl, gitHash, clientId } from './config'

const proxy = {}

if (process.env.NODE_ENV === 'development') {
  proxy['/api'] = api
  proxy['/pid'] = {
    target: api + '/api',
    changeOrigin: true,
    pathRewrite: {
      '^/pid': '/pid'
    }
  }
  proxy['/retrieve'] = {
    target: search,
    changeOrigin: true,
    pathRewrite: {
      '^/retrieve': ''
    }
  }
}

export default {
  target: 'server',
  ssr: false,

  telemetry: false,

  server: {
    port: 3000,
    host: '0.0.0.0',
    timing: false
  },

  head: {
    title,
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: '' }
    ],
    link: [
      { rel: 'icon', type: 'image/x-icon', href: icon }
    ]
  },

  css: [
    '@assets/globals.scss'
  ],

  plugins: [
    { src: '@/plugins/axios', ssr: false },
    { src: '@/plugins/toast', ssr: false },
    { src: '@/plugins/vendors', ssr: false },
    { src: '@/plugins/axios', ssr: false },
    { src: '@/plugins/vuex-persist.js', mode: 'client' }
  ],

  // Auto import components (https://go.nuxtjs.dev/config-components)
  components: true,

  buildModules: [
    '@nuxtjs/dotenv',
    '@nuxtjs/eslint-module',
    '@nuxtjs/vuetify'
  ],

  modules: [
    '@nuxtjs/proxy',
    '@nuxtjs/axios',
    ['nuxt-i18n', {
      locales: [
        { code: 'de', file: path.resolve(__dirname, 'locales/de-DE.json'), name: 'Deutsch' },
        { code: 'en', file: path.resolve(__dirname, 'locales/en-US.json'), name: 'English' }
      ],
      lazy: true,
      langDir: 'lang/',
      defaultLocale: 'en'
    }]
  ],

  axios: {
    proxy: proxy !== {}
  },

  proxy,

  publicRuntimeConfig: {
    sandbox,
    version,
    logo,
    clientId,
    clientSecret,
    defaultPublisher,
    doiUrl,
    baseUrl,
    gitHash
  },

  serverMiddleware: [
    { path: '/server-middleware', handler: path.resolve(__dirname, 'server-middleware/index.js') }
  ],

  vuetify: {
    customVariables: ['~/assets/variables.scss'],
    theme: {
      dark: false,
      themes: {
        light: {
          primary: colors.blue.darken2,
          accent: colors.amber.darken3,
          secondary: colors.blueGrey.base,
          info: colors.blue.lighten2,
          code: colors.grey.lighten4,
          warning: colors.orange.lighten2,
          error: colors.red.base /* is used by forms */,
          success: colors.teal.base
        },
        dark: {
          anchor: colors.blue.darken2
        }
      }
    }
  },

  build: {
    babel: {
      presets (env, [preset, options]) {
        return [
          ['@babel/preset-env', {
            targets: {
              node: 'current'
            }
          }]
        ]
      }
    }
  }
}
