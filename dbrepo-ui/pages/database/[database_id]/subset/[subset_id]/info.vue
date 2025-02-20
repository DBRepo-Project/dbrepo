<template>
  <div
    v-if="identifier || canViewInfo">
    <SubsetToolbar />
    <v-card
      variant="flat"
      rounded="0">
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
      v-if="canViewInfo && identifier" />
    <v-card
      v-if="canViewInfo"
      variant="flat"
      rounded="0"
      :title="$t('pages.subset.title')">
      <v-card-text>
        <v-list
          lines="two"
          dense>
          <v-list-item
            v-if="database"
            :title="$t('pages.subset.visibility.title')"
            density="compact">
            <ResourceStatus
              v-if="!identifier"
              :inline="true"
              :resource="database" />
            <ResourceStatus
              v-else
              :inline="true"
              :resource="identifier" />
          </v-list-item>
          <v-list-item
            v-if="subset.creator"
            :title="$t('pages.subset.creator.title')"
            density="compact">
            <UserBadge
              :user="subset.creator"
              :other-user="cacheUser" />
          </v-list-item>
          <v-list-item
            :title="$t('pages.subset.query.title')"
            density="compact">
            <pre>{{ subset.query }}</pre>
          </v-list-item>
          <v-list-item
            :title="`${$t('pages.subset.query.title')} ${$t('pages.subset.hash.title')}`"
            density="compact">
            <pre>{{ $t('pages.subset.hash.prefix') }}:{{ subset.query_hash }}</pre>
          </v-list-item>
          <v-list-item
            v-if="executionUTC"
            :title="$t('pages.subset.executed.title')"
            density="compact">
            {{ executionUTC }}
          </v-list-item>
          <v-list-item
            :title="`${$t('pages.subset.result.title')} ${$t('pages.subset.hash.title')}`"
            density="compact">
            <pre v-if="subset.result_hash">{{ $t('pages.subset.hash.prefix') }}:{{ subset.result_hash }}</pre>
            <span v-else>(none)</span>
          </v-list-item>
          <v-list-item
            :title="$t('pages.subset.rows.title')"
            density="compact">
            {{ subset.result_number }}
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const config = useRuntimeConfig()
const { pid } = useRoute().query
const { database_id, subset_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.client}/api/identifier?dbid=${database_id}&qid=${subset_id}&type=subset&status=published`)

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
import Summary from '@/components/identifier/Summary.vue'
import SubsetToolbar from '@/components/subset/SubsetToolbar.vue'
import Select from '@/components/identifier/Select.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    Select,
    Summary,
    SubsetToolbar,
    UserBadge
  },
  data () {
    return {
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
         title: this.$t('navigation.subsets'),
          to: `/database/${this.$route.params.database_id}/subset`
        },
        {
          title: `${this.$route.params.subset_id}`,
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/info`
        },
        {
          title: this.$t('navigation.info'),
          to: `/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/info`,
          disabled: true
        }
      ],
      persistQueryExists: false,
      persistQueryDialog: false,
      loadingDatabase: false,
      loadingIdentifier: false,
      downloadLoading: false,
      error: false,
      promises: [],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    pid () {
      return this.$route.query.pid
    },
    database () {
      return this.cacheStore.getDatabase
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    subset () {
      return this.cacheStore.getSubset
    },
    access () {
      return this.cacheStore.getAccess
    },
    identifiers () {
      if (!this.database || !this.database.subsets) {
        return []
      }
      return this.database.subsets.filter(i => i.query_id === this.$route.params.subset_id)
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
    title () {
      if (!this.identifier) {
        return null
      }
      const enTitle = this.identifier.titles.filter(t => t.language).filter(t => t.language === 'en')
      if (enTitle.length !== 1) {
        return this.identifier.titles[0].title
      }
      return enTitle[0].title
    },
    publisher () {
      if (this.database.publisher === null) {
        return 'NA'
      }
      return this.database.publisher
    },
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
    }
  }
}
</script>
