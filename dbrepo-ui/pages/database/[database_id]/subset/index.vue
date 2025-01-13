<template>
  <div
    v-if="canViewSchema">
    <DatabaseToolbar />
    <SubsetList />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'subset' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'subset' })" />
</template>

<script>
import SubsetList from '@/components/subset/SubsetList.vue'
import { errorCodeKey } from '@/utils'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    SubsetList
  },
  setup () {
    const config = useRuntimeConfig()
    const userStore = useUserStore()
    const { database_id } = useRoute().params
    const { error } = useFetch(`${config.public.api.server}/api/database/${database_id}`, {
      immediate: true,
      method: 'HEAD',
      timeout: 90_000,
      headers: {
        Accept: 'application/json',
        Authorization: userStore.getToken ? `Bearer ${userStore.getToken}` : null
      }
    })
    return {
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
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
         title: this.$t('navigation.subsets'),
          to: `/database/${this.$route.params.database_id}/subset`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    canViewSchema () {
      if (this.error) {
        return false
      }
      return this.database
    }
  },
  methods: {
    errorCodeKey
  }
}
</script>
