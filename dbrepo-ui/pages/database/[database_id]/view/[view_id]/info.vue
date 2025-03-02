<template>
  <div
    v-if="identifier || canViewInfo">
    <ViewToolbar />
    <v-window
      v-model="tab">
      <v-window-item>
        <v-card variant="flat">
          <Summary
            v-if="identifier"
            :identifier="identifier" />
          <v-card-text
            v-if="identifier">
            <Select
              :identifiers="identifiers"
              :identifier="identifier" />
          </v-card-text>
        </v-card>
        <v-divider
          v-if="identifier" />
        <v-card
          v-if="canViewInfo"
          :title="$t('pages.view.title')"
          variant="flat">
          <v-card-text>
            <v-list
              v-if="view"
              dense>
              <v-list-item
                :title="$t('pages.view.name.title')">
                {{ view.internal_name }}
              </v-list-item>
              <v-list-item
                :title="$t('pages.view.query.title')">
                <pre
                  class="line-break">{{ view.query }}</pre>
              </v-list-item>
              <v-list-item
                :title="$t('pages.view.owner.title')">
                <UserBadge
                  v-if="view"
                  :user="view.owner"
                  :other-user="cacheUser" />
                <v-skeleton-loader
                  v-else
                  type="subtitle"
                  width="200" />
              </v-list-item>
              <v-list-item
                v-if="view.created"
                :title="$t('pages.view.creation.title')">
                {{ formatUTC(view.created) }}
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const config = useRuntimeConfig()
const { pid } = useRoute().query
const { database_id, view_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.client}/api/identifier?dbid=${database_id}&vid=${view_id}&type=view&status=published`)

if (data.value && data.value.length > 0) {
  const identifierService = useIdentifierService()
  useServerHead(identifierService.identifiersToServerHead(data.value))
  useServerSeoMeta(identifierService.identifiersToServerSeoMeta(data.value))
}
const identifier = ref(data.value && data.value.length > 0 ? (pid && data.value.filter(i => i.id === pid).length > 0 ? data.value.filter(i => i.id === pid)[0] : data.value[0]) : null)

const cacheStore = useCacheStore()
cacheStore.setIdentifier(identifier)
</script>
<script>
import ViewToolbar from '@/components/view/ViewToolbar.vue'
import Summary from '@/components/identifier/Summary.vue'
import Select from '@/components/identifier/Select.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    Select,
    Summary,
    ViewToolbar,
    UserBadge
  },
  data () {
    return {
      tab: 0,
      loadingView: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}`
        },
        {
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/view`
        },
        {
          title: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
        },
        {
          title: this.$t('navigation.info'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/info`,
          disabled: true
        }
      ],
      error: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.cacheStore.getAccess
    },
    view () {
      return this.cacheStore.getView
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    identifiers () {
      if (!this.view || !this.view.identifiers) {
        return []
      }
      return this.view.identifiers.filter(i => i.query_id === this.$route.params.subset_id)
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.database.views
    },
    pid () {
      return this.$route.query.pid
    },
    creator () {
      if (!this.view) {
        return null
      }
      const userService = useUserService()
      return userService.userToFullName(this.view.creator)
    },
    canViewInfo () {
      if (!this.view) {
        return false
      }
      if (this.view.is_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  },
  methods: {
    formatUTC (timestamp) {
      return formatTimestampUTCLabel(timestamp)
    }
  }
}
</script>

<style>
pre {
  white-space: break-spaces;
}
.v-card__text {
  font-size: initial;
}
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
</style>
