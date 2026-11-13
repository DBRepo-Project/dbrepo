// Render the production components with real Vuetify controls and HTTP clients.
// Only the service responses and the surrounding Nuxt shell are supplied by the tests.
import {createApp, h, ref} from 'vue'
import {createPinia} from 'pinia'
import {createVuetify} from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import axios from 'axios'
import EditTuple from '../../components/dialogs/EditTuple.vue'
import {useTupleService} from '../../composables/tuple-service'
import {useTupleDraft} from '../../composables/tuple-draft'
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
  useCacheStore, useAccessService, useTupleService, useTupleDraft,
  useOidcAuth: () => ({user: {value: {provider: 'test', userInfo: {sub: 'owner'}}}}),
  useContainerService: () => ({findOne: () => Promise.resolve({image: {data_types: []}})}), useAxiosInstance: () => http, useRuntimeConfig: () => config,
  useToastInstance: () => toast,
  useUserService: () => ({findAll: () => http.get('/api/v1/user').then(r => r.data)}),
  useDatabaseService: () => ({findOne: () => http.get('/api/v1/database/db').then(r => r.data)}),
  useGrantService: () => ({findOne: () => Promise.resolve({type: 'read', grants: ['SELECT']})})
})
const mode = new URLSearchParams(location.search).get('mode')
const row = ref(null), table = ref(null), closed = ref(false)
const app = createApp({render: () => h(components.VApp, null, {default: () => mode === 'tuple'
  ? (row.value && table.value && !closed.value ? h(EditTuple, {tuple: row.value, table: table.value,
      edit: !new URLSearchParams(location.search).has('add'), onClose: () => { closed.value = true }}) : null)
  : h(Settings)})})
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

if (mode === 'tuple') {
  http.get('/api/v1/database/db/table/table').then(r => { table.value = r.data; store.setTable(r.data) })
  http.get('/api/v1/database/db/table/table/data').then(r => { row.value = r.data })
}
