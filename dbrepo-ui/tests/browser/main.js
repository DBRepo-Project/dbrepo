// Render the production components with real Vuetify controls and HTTP clients.
// Only the service responses and the surrounding Nuxt shell are supplied by the tests.
import {createApp, h} from 'vue'
import {createPinia} from 'pinia'
import {createVuetify} from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import axios from 'axios'
import Settings from '../../pages/database/[database_id]/settings.vue'
import {useCacheStore} from '../../stores/cache.js'
import {useAccessService} from '../../composables/access-service'
import messages from '../../locales/en-US.json'

const http = axios.create()
const config = {public: {variant: {input: {normal: 'underlined'}, button: {normal: 'flat'}},
  dashboard: {url: '/dashboard'}, database: {image: {width: 200, height: 200}}}}
const toast = Object.fromEntries(['success', 'error', 'warning', 'info'].map(type => [type, text => {
  const item = document.createElement('p'); item.dataset.toast = type; item.textContent = text
  document.body.append(item)
}]))
Object.assign(globalThis, {
  useCacheStore, useAccessService, useAxiosInstance: () => http, useRuntimeConfig: () => config,
  useToastInstance: () => toast,
  useUserService: () => ({findAll: () => http.get('/api/v1/user').then(r => r.data)}),
  useDatabaseService: () => ({findOne: () => http.get('/api/v1/database/db').then(r => r.data)}),
  useGrantService: () => ({findOne: () => Promise.resolve({type: 'read', grants: ['SELECT']})})
})
const app = createApp({render: () => h(components.VApp, null, {default: () => h(Settings)})})
app.use(createPinia()).use(createVuetify({components, directives}))
app.config.globalProperties.$route = {params: {database_id: 'db', table_id: 'table'}, path: '/database/db/settings'}
app.config.globalProperties.$t = (key, args = {}) => {
  let text = key.split('.').reduce((v, k) => v?.[k], messages) || key
  for (const [name, value] of Object.entries(args)) text = text.replaceAll(`{${name}}`, value)
  return text
}
const store = useCacheStore()
store.setRoles(new URLSearchParams(location.search).get('roles')?.split(',') || [
  'create-database-access', 'update-database-access', 'delete-database-access'])
store.setUser({preferred_username: 'owner'})
app.mount('#app')
http.get('/api/v1/database/db').then(r => store.setDatabase(r.data))
