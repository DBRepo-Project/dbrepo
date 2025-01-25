<template>
  <div
    v-if="canViewSchema">
    <DatabaseToolbar />
    <v-window
      v-model="tab">
      <v-window-item value="1">
        <Summary
          v-if="hasIdentifier"
          :identifier="identifier" />
        <v-card
          v-if="hasIdentifier"
          variant="flat"
          rounded="0">
          <v-card-text>
            <Select
              :identifiers="filteredIdentifiers"
              :identifier="identifier" />
          </v-card-text>
        </v-card>
        <v-divider
          v-if="hasIdentifier" />
        <v-card
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
                    :other-user="user" />
                </div>
              </v-list-item>
              <v-list-item
                v-if="database.contact"
                :title="$t('pages.database.contact.title')"
                density="compact">
                <div>
                  <UserBadge
                    :user="database.contact"
                    :other-user="user" />
                </div>
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

const { loggedIn, user, login, logout } = useOidcAuth()
</script>
<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import Summary from '@/components/identifier/Summary.vue'
import Select from '@/components/identifier/Select.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { sizeToHumanLabel } from '@/utils'
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
    description () {
      if (!this.hasIdentifier) {
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
      if (!this.hasIdentifier) {
        return ''
      }
      return this.database.identifier.publisher
    },
    database () {
      return this.cacheStore.getDatabase
    },
    identifiers () {
      if (!this.database) {
        return []
      }
      return this.database.identifiers
    },
    filteredIdentifiers () {
      if (!this.identifiers) {
        return []
      }
      if (!this.user) {
        return this.identifiers.filter(i => i.status === 'published')
      }
      return this.identifiers.filter(i => i.status === 'published' || i.owner.id === this.user.id)
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
    access () {
      return this.cacheStore.getAccess
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
    hasIdentifier () {
      return this.identifier
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
    canViewSchema () {
      if (this.error) {
        return false
      }
      return this.database
    }
  }
}
</script>

