<template>
  <div
    v-if="canViewSchema">
    <DatabaseToolbar />
    <v-window
      v-model="tab">
      <TableList />
    </v-window>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script>
import TableList from '@/components/table/TableList.vue'
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  name: 'Tables',
  components: {
    TableList,
    DatabaseToolbar
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
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`,
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
      if (!this.database) {
        return false
      }
      return this.database.is_schema_public
    }
  }
}
</script>

<style scoped>
a.table_from_csv {
  font-size: 14pt;
}
</style>
