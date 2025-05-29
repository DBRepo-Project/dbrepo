<template>
  <div
    v-if="identifier || canViewInfo">
    <DatabaseToolbar />
    <v-window
      v-model="tab">
      <v-window-item value="1">
        <Summary
          v-if="identifier"
          :identifier="identifier" />
        <v-card
          variant="flat"
          rounded="0">
          <v-card-text>
            <Select
              :identifiers="identifiers"
              :identifier="identifier" />
          </v-card-text>
        </v-card>
        <v-divider
          v-if="identifier" />
        <v-card
          v-if="canViewInfo"
          :title="$t('pages.database.title')"
          variant="flat"
          rounded="0">
          <v-card-text>
            <v-skeleton-loader
              v-if="!database"
              type="list-item-three-line"
              width="50%" />
            <v-list
              v-if="database"
              lines="two"
              dense>
              <v-list-item
                v-if="previewImage"
                :title="$t('pages.database.image.title')"
                density="compact">
                <v-img
                  :src="previewImage"
                  :alt="$t('pages.database.image.alt')"
                  :title="$t('pages.database.image.alt')"
                  :max-width="maxWidth"
                  :max-height="maxHeight" />
              </v-list-item>
              <v-list-item
                v-if="canViewDashboard"
                :title="$t('pages.database.dashboard.title')"
                density="compact">
                <NuxtLink
                  target="_blank"
                  :href="`${config.public.dashboard.url}/d/${database.dashboard_uid}`">
                  {{ $t('pages.database.dashboard.text') }}
                </NuxtLink>
              </v-list-item>
              <v-list-item
                :title="$t('pages.database.name.title')"
                density="compact">
                <div>
                  {{ database.name }}
                </div>
              </v-list-item>
              <v-list-item
                :title="$t('pages.database.internal-name.title')"
                density="compact">
                <div>
                  {{ database.internal_name }}
                </div>
              </v-list-item>
              <v-list-item
                v-if="databaseSize"
                :title="$t('pages.database.size.title')"
                density="compact">
                <div>
                  {{ databaseSize }}
                </div>
              </v-list-item>
              <v-list-item
                v-if="access && access.type"
                :title="$t('pages.database.subpages.access.title')"
                density="compact">
                <div v-if="access && access.type">
                  <span>
                    <v-badge
                      v-if="databaseExtraInfo"
                      inline
                      :content="databaseExtraInfo"
                      color="secondary">
                      <span>
                        {{ accessDescription.text }}
                      </span>
                    </v-badge>
                    <span
                      v-else>
                      {{ accessDescription.text }}
                    </span>
                  </span>
                </div>
              </v-list-item>
              <v-list-item
                :title="$t('pages.database.owner.title')"
                density="compact">
                <div>
                  <UserBadge
                    :user="database.owner"
                    :other-user="cacheUser" />
                </div>
              </v-list-item>
              <v-list-item
                v-if="database.contact"
                :title="$t('pages.database.contact.title')"
                density="compact">
                <div>
                  <UserBadge
                    :user="database.contact"
                    :other-user="cacheUser" />
                </div>
              </v-list-item>
              <v-list-item
                v-if="database.created"
                :title="$t('pages.database.creation.title')">
                {{ formatUTC(database.created) }}
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          :title="$t('pages.container.title')"
          variant="flat"
          rounded="0">
          <v-card-text>
            <v-skeleton-loader
              v-if="!database"
              type="list-item-three-line"
              width="50%" />
            <v-list
              v-if="database"
              lines="two"
              dense>
              <v-list-item
                :title="$t('pages.container.name.title')"
                density="compact">
                <div>
                  {{ container_name }}
                </div>
              </v-list-item>
              <v-list-item
                :title="$t('pages.container.internal-name.title')"
                density="compact">
                <div>
                  {{ container_internal_name }}
                </div>
              </v-list-item>
              <v-list-item
                :title="$t('pages.container.image-name.title')"
                density="compact">
                <div>
                  {{ image_name }}
                </div>
              </v-list-item>
              <v-list-item
                :title="$t('pages.container.image-tag.title')"
                density="compact">
                <div>
                  {{ image_version }}
                </div>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const config = useRuntimeConfig()
const { pid } = useRoute().query
const { database_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.client}/api/identifier?dbid=${database_id}&type=database&status=published`)

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
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import Summary from '@/components/identifier/Summary.vue'
import Select from '@/components/identifier/Select.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { formatTimestampUTCLabel, sizeToHumanLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    DatabaseToolbar,
    Summary,
    Select,
    UserBadge
  },
  data () {
    return {
      error: null,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.info'),
          to: `/database/${this.$route.params.database_id}/info`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    tab () {
      return 0
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    description () {
      if (!this.identifier) {
        return ''
      }
      return this.database.identifier.description
    },
    maxWidth () {
      return this.$config.public.database.image.width
    },
    maxHeight () {
      return this.$config.public.database.image.height
    },
    publisher () {
      if (!this.identifier) {
        return ''
      }
      return this.database.identifier.publisher
    },
    database () {
      return this.cacheStore.getDatabase
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    access () {
      return this.cacheStore.getAccess
    },
    identifiers () {
      if (!this.database || !this.database.identifiers) {
        return []
      }
      return this.database.identifiers.filter(i => i.database_id === this.$route.params.database_id)
    },
    pid () {
      return this.$route.query.pid
    },
    internal_name () {
      if (!this.database) {
        return
      }
      return this.database.internal_name
    },
    container_name () {
      if (!this.database) {
        return
      }
      return this.database.container.name
    },
    container_internal_name () {
      if (!this.database) {
        return
      }
      return this.database.container.internal_name
    },
    image_name () {
      if (!this.database) {
        return
      }
      return this.database.container.image.name
    },
    image_version () {
      if (!this.database) {
        return
      }
      return this.database.container.image.version
    },
    contact () {
      const databaseService = useDatabaseService()
      return databaseService.databaseToContact(this.database)
    },
    owner () {
      const databaseService = useDatabaseService()
      return databaseService.databaseToOwner(this.database)
    },
    accessDescription () {
      if (!this.access) {
        return
      }
      switch (this.access.type) {
        case 'read':
          return { text: this.$t('pages.database.subpages.access.read') }
        case 'write_own':
          return { text: this.$t('pages.database.subpages.access.write-own') }
        case 'write_all':
          return { text: this.$t('pages.database.subpages.access.write-all') }
        default:
          return { text: null, class: null }
      }
    },
    databaseExtraInfo () {
      return this.$config.public.database.extra
    },
    databaseSize () {
      if (!this.database) {
        return null
      }
      let sum = 0
      this.database.tables.forEach((t) => { sum += t.data_length })
      return sizeToHumanLabel(sum)
    },
    previewImage () {
      if (!this.database) {
        return null
      }
      return this.database.preview_image
    },
    canViewInfo () {
      if (!this.database) {
        return false
      }
      if (this.database.is_public || this.database.is_schema_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    },
    canViewDashboard () {
      if (!this.database || !this.database.views) {
        return false
      }
      if (!this.database.is_public && !this.database.is_schema_public) {
        return false
      }
      return this.database.dashboard_uid
    }
  },
  methods: {
    formatUTC (timestamp) {
      return formatTimestampUTCLabel(timestamp)
    }
  }
}
</script>

