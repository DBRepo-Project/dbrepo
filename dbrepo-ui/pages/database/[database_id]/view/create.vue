<template>
  <div
    v-if="canCreateView">
    <Builder mode="view" />
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
import Builder from '@/components/subset/Builder.vue'
import JumboBox from '@/components/JumboBox.vue'
import { useUserStore } from '@/stores/user'
import { errorCodeKey } from '@/utils'

export default {
  components: {
    Builder,
    JumboBox
  },
  setup () {
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
          title: this.$t('navigation.create'),
          to: `/database/${this.$route.params.database_id}/view/create`,
          disabled: true
        }
      ],
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    canCreateView () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database-view')
    }
  },
  methods: {
    errorCodeKey
  }
}
</script>
