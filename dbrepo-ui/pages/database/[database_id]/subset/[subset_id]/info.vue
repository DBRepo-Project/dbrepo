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
          v-if="subset"
          lines="two"
          dense>
          <v-list-item
            v-if="database"
            :title="$t('pages.subset.visibility.title')"
            density="compact">
            {{ database.is_public ? $t('toolbars.database.public') : $t('toolbars.database.private') }}
          </v-list-item>
          <v-list-item
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
            :title="$t('pages.subset.query-hash.title')"
            density="compact">
            <pre v-text="`${this.$t('pages.subset.query-hash.prefix')}${subset.query_hash}`" />
          </v-list-item>
          <v-list-item
            v-if="executionUTC"
            :title="$t('pages.subset.executed.title')"
            density="compact">
            {{ executionUTC }}
          </v-list-item>
          <v-list-item
            :title="$t('pages.subset.result-hash.title')"
            density="compact">
            <pre v-text="result_hash" />
          </v-list-item>
          <v-list-item
            :title="$t('pages.subset.result-rows.title')"
            density="compact">
            {{ subset.result_number }}
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <v-divider />
    <v-card
      :title="$t('pages.database.title')"
      variant="flat"
      rounded="0">
      <v-card-text>
        <v-list
          v-if="database"
          dense>
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
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script setup>
const config = useRuntimeConfig()
const { database_id, subset_id } = useRoute().params
const { data } = await useFetch(`${config.public.api.server}/api/database/${database_id}/query/${subset_id}`)
if (data.value) {
  const identifierService = useIdentifierService()
  useServerHead(identifierService.subsetToServerHead(data.value))
  useServerSeoMeta(identifierService.subsetToServerSeoMeta(data.value))
}
</script>
<script>
import Summary from '@/components/identifier/Summary'
import SubsetToolbar from '@/components/subset/SubsetToolbar'
import Select from '@/components/identifier/Select'
import UserBadge from '@/components/user/UserBadge'
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
      loadingSubset: true,
      downloadLoading: false,
      error: false,
      promises: [],
      subset: null,
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
    result_hash () {
      if (!this.subset.result_hash) {
        return '(none)'
      }
      return `sha256:${this.subset.result_hash}`
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
  },
  mounted () {
    this.loadSubset()
  },
  methods: {
    loadSubset () {
      this.loadingSubset = true
      const queryService = useQueryService()
      queryService.findOne(this.$route.params.database_id, this.$route.params.subset_id)
        .then((subset) => {
          this.subset = subset
        })
        .catch(() => {
          this.loadingSubset = false
        })
        .finally(() => {
          this.loadingSubset = false
        })
    }
  }
}
</script>
