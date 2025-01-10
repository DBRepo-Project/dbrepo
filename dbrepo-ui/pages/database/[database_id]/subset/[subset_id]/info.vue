<template>
  <div>
    <SubsetToolbar />
    <v-card
      variant="flat"
      rounded="0">
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
      v-if="subset && identifier" />
    <v-card
      variant="flat"
      rounded="0"
      :title="$t('pages.subset.title')">
      <v-card-text>
        <v-list
          v-if="!subset"
          lines="two"
          dense>
          <v-skeleton-loader
            type="list-item-three-line"
            width="50%" />
        </v-list>
        <v-list
          v-else-if="subset"
          lines="two"
          dense>
          <v-list-item
            v-if="database"
            :title="$t('pages.subset.visibility.title')"
            density="compact">
            <ResourceStatus
              :inline="true"
              :resource="subset" />
          </v-list-item>
          <v-list-item
            v-if="subset.creator"
            :title="$t('pages.subset.creator.title')"
            density="compact">
            <UserBadge :user="subset.creator" :other-user="user" />
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

<script>
import Summary from '@/components/identifier/Summary.vue'
import SubsetToolbar from '@/components/subset/SubsetToolbar.vue'
import Select from '@/components/identifier/Select.vue'
import UserBadge from '@/components/user/UserBadge.vue'
import { formatTimestampUTCLabel } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    Select,
    Summary,
    SubsetToolbar,
    UserBadge
  },
  setup () {
    const config = useRuntimeConfig()
    const userStore = useUserStore()
    const { database_id, subset_id } = useRoute().params
    const { error, data } = useFetch(`${config.public.api.server}/api/database/${database_id}/subset/${subset_id}`, {
      immediate: true,
      timeout: 90_000,
      headers: {
        Accept: 'application/json',
        Authorization: userStore.getToken ? `Bearer ${userStore.getToken}` : null
      }
    })
    if (data.value) {
      const identifierService = useIdentifierService()
      useServerHead(identifierService.subsetToServerHead(data.value))
      useServerSeoMeta(identifierService.subsetToServerSeoMeta(data.value))
    }
    return {
      subset: data,
      error
    }
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
      userStore: useUserStore(),
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
    access () {
      return this.userStore.getAccess
    },
    user () {
      return this.userStore.getUser
    },
    identifiers () {
      if (!this.database || !this.database.subsets || this.database.subsets.length === 0) {
        return []
      }
      return this.database.subsets.filter(s => s.query_id === Number(this.$route.params.subset_id))
    },
    hasIdentifier () {
      return this.identifiers.length > 0
    },
    identifier () {
      if (this.pid) {
        const filter = this.identifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.identifiers[0]
    },
    title () {
      if (!this.hasIdentifier) {
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
