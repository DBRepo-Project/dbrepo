<template>
  <div>
    <v-list-item
      v-for="(id, i) in identifiers"
      :key="`i-${i}`"
      :value="idx"
      :active="isActive(id)"
      color="primary"
      :variant="listVariant"
      :href="href(id)"
      :title="formatTimestampUTCLabel(id.created)"
      lines="two">
      <v-list-item-subtitle>
        <Banner
          :identifier="id" />
      </v-list-item-subtitle>
      <template v-slot:append>
        <v-tooltip
          :text="$t('pages.identifier.pid.title')"
          left>
          <template v-slot:activator="{ props }">
            <v-icon
              color="primary"
              v-bind="props">mdi-identifier</v-icon>
          </template>
        </v-tooltip>
      </template>
    </v-list-item>
  </div>
</template>

<script>
import Banner from '@/components/identifier/Banner'
import { formatTimestampUTCLabel } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
      localIdentifier: null,
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    canDeleteIdentifier () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('delete-identifier')
    },
    listVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.list.contrast : runtimeConfig.public.variant.list.normal
    },
  },
  watch: {
    identifier: {
      handler () {
        this.init()
      },
      deep: true
    },
    idx: {
      handler () {
        this.localIdentifier = this.identifiers[this.idx]
      }
    }
  },
  mounted () {
    this.init()
  },
  methods: {
    formatTimestampUTCLabel,
    href (identifier) {
      if (this.canDeleteIdentifier) {
        return null
      }
      return `/pid/${identifier.id}`
    },
    isActive (identifier) {
      if (!identifier) {
        return false
      }
      return this.identifier.id === identifier.id
    },
    init () {
      if (!this.identifiers || this.identifiers.length === 0 || !this.identifier) {
        return null
      }
      this.idx = this.identifiers.map(i => i.id).indexOf(this.identifier.id)
      this.localIdentifier = this.identifier
    }
  }
}
</script>
