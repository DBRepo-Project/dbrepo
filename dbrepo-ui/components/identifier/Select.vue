<template>
  <div>
    <v-list-item
      v-for="(identifier, i) in displayIdentifiers"
      :key="`i-${i}`"
      :value="idx"
      :active="isActive(identifier)"
      :color="color(identifier)"
      :variant="listVariant"
      :href="href(identifier)"
      :title="formatTimestampUTCLabel(identifier.created)"
      lines="two">
      <v-list-item-subtitle>
        <Banner
          :identifier="identifier" />
      </v-list-item-subtitle>
      <template v-slot:append>
        <v-tooltip
          v-if="identifier.status === 'published'"
          :text="$t('pages.identifier.pid.title')"
          left>
          <template
            v-slot:activator="{ props }">
            <v-icon
              color="primary"
              v-bind="props">mdi-identifier</v-icon>
          </template>
        </v-tooltip>
        <v-tooltip
          v-else
          :text="$t('pages.identifier.draft.title')"
          left>
          <template
            v-slot:activator="{ props }">
            <v-icon
              v-bind="props">mdi-pencil-outline</v-icon>
          </template>
        </v-tooltip>
      </template>
    </v-list-item>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const { loggedIn, user, login, logout } = useOidcAuth()
const userInfo = ref(loggedIn ? user.value?.userInfo : null)
const roles = ref(loggedIn ? user.value?.claims?.realm_access?.roles : [])
</script>
<script>
import Banner from '@/components/identifier/Banner.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    Banner
  },
  props: {
    identifiers: {
      type: Array,
      default () {
        return []
      }
    },
    identifier: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      idx: null,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    displayIdentifiers () {
      if (!this.identifiers) {
        return []
      }
      if (!this.user) {
        return this.identifiers.filter(i => i.status === 'published')
      }
      return this.identifiers.filter(i => i.status === 'published' || i.owner.id === this.user.id)
    },
    listVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.list.contrast : runtimeConfig.public.variant.list.normal
    }
  },
  watch: {
    identifier: {
      handler () {
        this.init()
      },
      deep: true
    }
  },
  mounted () {
    this.init()
  },
  methods: {
    formatTimestampUTCLabel,
    href (identifier) {
      if (identifier.status === 'published') {
        return `/pid/${identifier.id}`
      }
      switch (identifier.type) {
        case 'database':
          return `/database/${identifier.database_id}/persist/${identifier.id}`
        case 'subset':
          return `/database/${identifier.database_id}/subset/${identifier.query_id}/persist/${identifier.id}`
        case 'table':
          return `/database/${identifier.database_id}/table/${identifier.table_id}/persist/${identifier.id}`
        case 'view':
          return `/database/${identifier.database_id}/view/${identifier.view_id}/persist/${identifier.id}`
      }
    },
    isActive (identifier) {
      if (!identifier) {
        return false
      }
      return this.identifier.id === identifier.id
    },
    color (identifier) {
      if (!identifier) {
        return false
      }
      return identifier.status === 'published' ? 'primary' : null
    },
    init () {
      if (!this.identifiers) {
        return null
      }
      this.idx = this.identifiers.map(i => i.id).indexOf(this.identifier.id)
    }
  }
}
</script>
