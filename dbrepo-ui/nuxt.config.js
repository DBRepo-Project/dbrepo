import path from 'path'
import colors from 'vuetify/es5/util/colors'
import { forceSsl, icon, clientSecret, title, logo, version, defaultPublisher, doiUrl, minIoUrl, clientId, searchUsername, searchPassword, brokerLoginUrl, keycloakLoginUrl, openSearchUrl, s3storageHostname, s3storagePort, s3accessKeyId, s3secretAccessKey } from './config'

const proxy = {}

const api = 'http://localhost'

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
    target: api + '/retrieve',
    changeOrigin: true,
    pathRewrite: {
      '^/retrieve': ''
    }
  }
}

const meta = [
  { charset: 'utf-8' },
  { name: 'viewport', content: 'width=device-width, initial-scale=1' }
]

if (forceSsl) {
  console.info('Flag FORCE_SSL is set: http-equiv Content-Security-Policy header is set to upgrade-insecure-requests')
  meta.push({ 'http-equiv': 'Content-Security-Policy', content: 'upgrade-insecure-requests' })
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
    meta,
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
    title,
    version,
    logo,
    clientId,
    clientSecret,
    defaultPublisher,
    brokerLoginUrl,
    keycloakLoginUrl,
    openSearchUrl,
    searchUsername,
    searchPassword,
    doiUrl,
    minIoUrl,
    s3storageHostname,
    s3storagePort,
    s3accessKeyId,
    s3secretAccessKey
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
          success: colors.green.base
        },
        dark: {
          anchor: colors.blue.darken2
        }
      }
    }
  },

  // https://github.com/nuxt/nuxt/issues/7722
  build: {
    extend (config, { isDev, isClient }) {
      /* AWS S3 depends on this, we need to tell it that we are a client, not a server */
      config.node = {
        fs: 'empty'
      }
    },
    babel: {
      presets (env, [preset, options]) {
        return [
          ['@babel/preset-env', {
            targets: {
              node: '14'
            }
          }]
        ]
      }
    }
  }
}
