// plugins/vuetify.js
import { createVuetify, type ThemeDefinition } from 'vuetify'
import colors from 'vuetify/util/colors'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import '@mdi/font/css/materialdesignicons.css'

const tuwThemeLight: ThemeDefinition = {
  dark: false,
  colors: {
    background: colors.grey.lighten3,
    surface: colors.grey.lighten5,
    'surface-light': colors.grey.lighten5,
    'surface-variant': colors.grey.base,
    primary: colors.blue.darken2,
    secondary: colors.blueGrey.darken2,
    tertiary: colors.grey.lighten2,
    error: colors.red.base,
    info: colors.blue.lighten2,
    success: colors.green.base,
    warning: colors.orange.lighten2
  },
}

const tuwThemeLightContrast: ThemeDefinition = {
  dark: false,
  colors: {
    background: colors.grey.lighten2,
    surface: colors.grey.lighten5,
    'surface-light': colors.grey.lighten5,
    'surface-variant': colors.grey.darken2,
    primary: colors.blue.darken2,
    secondary: colors.blueGrey.darken2,
    tertiary: colors.grey.lighten2,
    error: colors.red.base,
    info: colors.blue.lighten2,
    success: colors.green.base,
    warning: colors.orange.lighten2
  }
}

const tuwThemeDark: ThemeDefinition = {
  dark: true,
  colors: {
    background: colors.grey.darken4,
    surface: '#1a1a1a', /* darken5 */
    'surface-light':'#1a1a1a', /* darken5 */
    'surface-variant': colors.grey.base,
    primary: colors.blue.darken2,
    secondary: colors.blueGrey.darken2,
    tertiary: colors.grey.darken2,
    error: colors.red.base,
    info: colors.blue.darken2,
    success: colors.green.base,
    warning: colors.orange.lighten2
  },
}

const tuwThemeDarkContrast: ThemeDefinition = {
  dark: true,
  colors: {
    background: colors.grey.darken4,
    surface: '#141414', /* darken6 */
    'surface-light':'#141414', /* darken6 */
    'surface-variant': colors.grey.base,
    primary: colors.blue.darken2,
    secondary: colors.blueGrey.darken2,
    tertiary: colors.grey.darken2,
    error: colors.red.base,
    info: colors.blue.darken2,
    success: colors.green.darken2,
    warning: colors.orange.lighten2
  },
}

export default defineNuxtPlugin(app => {
  const vuetify : any = createVuetify({
    ssr: true,
    components,
    directives,
    defaults: {
      VSelect: {
        variant: 'underlined'
      },
      VTextField: {
        variant: 'underlined'
      }
    },
    theme: {
      defaultTheme: 'tuwThemeLight',
      themes: {
        tuwThemeLight,
        tuwThemeLightContrast,
        tuwThemeDark,
        tuwThemeDarkContrast
      }
    }
  })

  app.vueApp.use(vuetify)
})
