<template>
  <div
    v-if="canViewSchema">
    <DatabaseToolbar />
    <v-window
      v-model="tab">
      <ViewList />
    </v-window>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'view' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'view' })" />
</template>

<script setup>
import { ref } from 'vue'

const runtimeConfig = useRuntimeConfig()
const config = ref(runtimeConfig)
</script>
<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import JumboBox from '@/components/JumboBox.vue'
import ViewList from '@/components/view/ViewList.vue'
import { useCacheStore } from '@/stores/cache'
import { errorCodeKey } from '@/utils'

export default {
  name: 'Views',
  components: {
    ViewList,
    DatabaseToolbar,
    JumboBox
  },
  setup () {
    const config = useRuntimeConfig()
    const userStore = useUserStore()
    const { database_id } = useRoute().params
    const { error } = useFetch(`${this.config.public.api.server}/api/database/${database_id}`, {
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
      tab: 0,
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
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/view`,
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
