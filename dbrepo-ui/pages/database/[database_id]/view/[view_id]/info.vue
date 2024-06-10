<template>
  <div>
    <ViewToolbar />
    <v-window
      v-model="tab">
      <v-window-item
        v-if="view">
        <v-card variant="flat">
          <Summary
            v-if="hasIdentifier"
            :identifier="identifier" />
          <v-card-text
            v-if="hasIdentifier">
            <Select
              :identifiers="identifiers"
              :identifier="identifier" />
          </v-card-text>
        </v-card>
        <v-divider
          v-if="hasIdentifier" />
        <v-card
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
                <pre>{{ view.query }}</pre>
              </v-list-item>
              <v-list-item
                :title="$t('pages.view.creator.title')">
                <UserBadge
                  :user="view.creator"
                  :other-user="user" />
              </v-list-item>
              <v-list-item
                v-if="view.created"
                :title="$t('pages.view.creation.title')">
                {{ formatUTC(view.created) }}
              </v-list-item>
              <v-list-item
                :title="$t('pages.view.visibility.title')">
                {{ view.is_public ? $t('toolbars.database.public') : $t('toolbars.database.private') }}
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          :title="$t('pages.database.title')"
          variant="flat">
          <v-card-text>
            <v-list dense>
              <v-list-item
                :title="$t('pages.database.visibility.title')">
                {{ database.is_public ? $t('toolbars.database.public') : $t('toolbars.database.private') }}
              </v-list-item>
              <v-list-item
                :title="$t('pages.database.name.title')">
                <NuxtLink
                  class="text-primary"
                  :to="`/database/${database.id}`"
                  v-text="database.internal_name" />
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
const config = useRuntimeConfig()
const { database_id, view_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.server}/api/database/${database_id}/view/${view_id}`)
if (data.value) {
  const identifierService = useIdentifierService()
  useServerHead(identifierService.viewToServerHead(data.value))
  useServerSeoMeta(identifierService.viewToServerSeoMeta(data.value))
}
</script>
<script>
import ViewToolbar from '@/components/view/ViewToolbar.vue'
import Summary from '@/components/identifier/Summary.vue'
import Select from '@/components/identifier/Select.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
    database () {
      return this.cacheStore.getDatabase
    },
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    access () {
      return this.userStore.getAccess
    },
    identifiers () {
      if (!this.view) {
        return []
      }
      return this.view.identifiers
    },
    filteredIdentifiers () {
      if (!this.identifiers) {
        return []
      }
      if (!this.user) {
        return this.identifiers.filter(i => i.status === 'published')
      }
      return this.identifiers.filter(i => i.status === 'published' || i.creator.id === this.user.id)
    },
    identifier () {
      if (this.pid) {
        const filter = this.filteredIdentifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.filteredIdentifiers[0]
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
    hasIdentifier () {
      return this.identifier
    },
    creator () {
      if (!this.view) {
        return null
      }
      const userService = useUserService()
      return userService.userToFullName(this.view.creator)
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
.skeleton-large > div {
  width: 400px !important;
}
.skeleton-medium > div {
  width: 200px !important;
}
.skeleton-small > div {
  width: 100px !important;
}
.skeleton-xsmall > div {
  width: 50px !important;
}
</style>
