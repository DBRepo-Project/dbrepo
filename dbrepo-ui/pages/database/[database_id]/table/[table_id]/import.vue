<template>
  <div
    v-if="canInsertTableData">
    <v-toolbar flat>
      <v-btn
        class="mr-2"
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/table`" />
      <v-toolbar-title
        :text="title" />
    </v-toolbar>
    <v-card
      variant="flat"
      rounded="0">
      <v-card-text>
        <v-stepper
          vertical
          variant="flat">
          <TableImport
            :create="false"
            :table-id="$route.params.table_id" />
        </v-stepper>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
  <JumboBox
    v-if="error"
    :title="$t(errorCodeKey(error).title, { resource: 'table' })"
    :subtitle="$t(errorCodeKey(error).subtitle)"
    :text="$t(errorCodeKey(error).text, { resource: 'table' })" />
</template>

<script setup>
import { ref } from 'vue'

const runtimeConfig = useRuntimeConfig()
const config = ref(runtimeConfig)
</script>
<script>
import TableImport from '@/components/table/TableImport.vue'
import JumboBox from '@/components/JumboBox.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'
import { errorCodeKey } from '@/utils'

export default {
  components: {
    TableImport,
    JumboBox
  },
  setup () {
    const userStore = useUserStore()
    const { database_id, table_id } = useRoute().params
    const { error } = useFetch(`${this.config.public.api.server}/api/database/${database_id}/table/${table_id}`, {
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
      step: 1,
      ready: false,
      file: {
        filename: null,
        path: null
      },
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
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/info`
        },
        {
          title: this.$t('navigation.import'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/import`,
          disabled: true
        }
      ],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    table () {
      return this.cacheStore.getTable
    },
    title () {
      if (!this.table) {
        return this.$t('pages.table.import.title')
      }
      return this.$t('pages.table.import.title') + ' ' + this.table.name
    },
    canInsertTableData () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
    }
  },
  methods: {
    errorCodeKey
  }
}
</script>
