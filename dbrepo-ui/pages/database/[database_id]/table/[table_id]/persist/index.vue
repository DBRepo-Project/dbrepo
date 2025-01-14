<template>
  <div
    v-if="canPersistTable">
    <Persist
      type="table"
      :database="database"
      :table="table" />
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'identifier' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'identifier' })" />
</template>

<script setup>
import { ref } from 'vue'

const runtimeConfig = useRuntimeConfig()
const config = ref(runtimeConfig)
</script>
<script>
import Persist from '@/components/identifier/Persist.vue'
import JumboBox from '@/components/JumboBox.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import { errorCodeKey } from '@/utils'

export default {
  components: {
    Persist,
    JumboBox
  },
  setup () {
    const userStore = useUserStore()
    const { database_id, table_id } = useRoute().params
    const { error, data } = useFetch(`${this.config.public.api.server}/api/database/${database_id}/table/${table_id}`, {
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
      loading: false,
      query: null,
      isAuthorizationError: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/persist`,
          disabled: true
        }
      ],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    roles () {
      return this.userStore.getRoles
    },
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.userStore.getAccess
    },
    table () {
      return this.cacheStore.getTable
    },
    canPersistTable () {
      if (!this.table) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
    }
  },
  methods: {
    errorCodeKey
  }
}
</script>
