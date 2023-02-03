import path from 'path'
import colors from 'vuetify/es5/util/colors'

// pick env vars from .env file or get them passed through docker-compose
require('dotenv').config()

if (process.env.SANDBOX) {
  console.info('[FDA] Running in sandbox environment')
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
    titleTemplate: '%s - Database Repository (Sandbox)',
    title: 'FAIR Data Austria',
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: '' }
    ],
    link: [
      { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }
    ]
  },

  css: [
    '@assets/globals.scss'
  ],

  plugins: [
    { src: '~/plugins/toast', ssr: false },
    { src: '~/plugins/vendors', ssr: false },
    { src: '~/plugins/axios' },
    { src: '~/plugins/vuex-persist.js', mode: 'client' }
  ],

  // Auto import components (https://go.nuxtjs.dev/config-components)
  components: true,

  buildModules: [
    '@nuxtjs/eslint-module',
    '@nuxtjs/vuetify'
  ],

  modules: [
    '@nuxtjs/proxy',
    '@nuxtjs/axios',
    ['nuxt-i18n', {
      locales: [
        { code: 'de', file: 'de-DE.js', name: 'Deutsch' },
        { code: 'en', file: 'en-US.js', name: 'English' }
      ],
      lazy: true,
      langDir: 'lang/',
      defaultLocale: 'en'
    }]
  ],

  axios: {
    proxy: true
  },

  publicRuntimeConfig: {
    brokerUsername: process.env.BROKER_USERNAME || 'fda',
    brokerPassword: process.env.BROKER_PASSWORD || 'fda',
    sandbox: process.env.SANDBOX || false,
    sharedFilesystem: process.env.SHARED_FILESYSTEM || '/tmp',
    version: process.env.VERSION || 'latest',
    logo: process.env.LOGO || '/logo.png',
    mailVerify: process.env.MAIL_VERIFY || false,
    tokenMax: process.env.TOKEN_MAX || 5,
    elasticPassword: process.env.ELASTIC_PASSWORD || 'elastic'
  },

  proxy: {
    '/api': process.env.API || 'http://localhost:9095',
    '/pid': {
      target: process.env.API + '/api' || 'http://localhost:9095/api',
      changeOrigin: true,
      pathRewrite: {
        '^/pid': '/pid'
      }
    },
    '/retrieve': {
      target: process.env.SEARCH || 'http://localhost:9200',
      changeOrigin: true,
      pathRewrite: {
        '^/retrieve': ''
      }
    }
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
          info: colors.amber.lighten4,
          code: colors.grey.lighten4,
          warning: colors.orange.lighten2,
          error: colors.red.base /* is used by forms */,
          banner: colors.red.lighten2,
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
