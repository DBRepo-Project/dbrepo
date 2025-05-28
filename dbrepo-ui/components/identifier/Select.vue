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
      :title="title(identifier)"
      lines="two">
      <v-list-item-subtitle>
        <Banner
          :identifier="identifier" />
      </v-list-item-subtitle>
      <template v-slot:append>
        <v-list-item-action>
          <v-tooltip
            :text="identifier.status === 'published' ? $t('pages.identifier.pid.title') : $t('pages.identifier.draft.title')"
            left>
            <template
              v-slot:activator="{ props }">
              {{ formatTimestampUTCLabel(identifier.created) }}
              <v-icon
                :color="identifier.status === 'published' ? 'primary' : null"
                v-bind="props">
                {{ identifier.status === 'published' ? 'mdi-identifier' : 'mdi-pencil-outline' }}
              </v-icon>
            </template>
          </v-tooltip>
        </v-list-item-action>
      </template>
    </v-list-item>
  </div>
</template>

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
        return null
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
    cacheUser () {
      return this.cacheStore.getUser
    },
    displayIdentifiers () {
      if (!this.identifiers || this.identifiers.length === 0) {
        if (!this.identifier) {
          return []
        }
        return [this.identifier]
      }
      if (!this.cacheUser) {
        return this.identifiers.filter(i => i.status === 'published')
      }
      return this.identifiers.filter(i => i.status === 'published' || i.owner.id === this.cacheUser.uid)
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
      if (!identifier) {
        return null
      }
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
    title (identifier) {
      if (!identifier) {
        return null
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierPreferEnglishTitle(identifier)
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
