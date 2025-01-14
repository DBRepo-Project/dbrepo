<template>
  <div
    v-if="canPersistView">
    <Persist
      type="view"
      :database="database"
      :view="view" />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
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
    const { database_id, view_id } = useRoute().params
    const { error } = useFetch(`${this.config.public.api.server}/api/database/${database_id}/view/${view_id}`, {
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
      isAuthorizationError: false,
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
          to: `/database/${this.$route.params.database_id}/view`
        },
        {
          title: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
        },
        {
          title: this.$t('navigation.persist'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/persist`,
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
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    access () {
      return this.userStore.getAccess
    },
    canPersistView () {
      if (!this.view) {
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
