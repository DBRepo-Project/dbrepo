import path from 'path'
import colors from 'vuetify/es5/util/colors'
import { sandbox, title, icon, brokerUsername, brokerPassword, sharedFilesystem, version, logo, mailVerify, tokenMax, elasticPassword, clientSecret, api, search } from './config'

if (sandbox) {
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
    { src: '@/plugins/toast', ssr: false },
    { src: '@/plugins/vendors', ssr: false },
    { src: '@/plugins/axios', ssr: false },
    { src: '@/plugins/vuex-persist.js', mode: 'client' }
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
    brokerUsername,
    brokerPassword,
    sandbox,
    sharedFilesystem,
    version,
    logo,
    mailVerify,
    tokenMax,
    elasticPassword,
    clientSecret
  },

  proxy: {
    '/api': api,
    '/pid': {
      target: process.env.API + '/api' || 'http://localhost:9095/api',
      changeOrigin: true,
      pathRewrite: {
        '^/pid': '/pid'
      }
    },
    '/retrieve': {
      target: search,
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
