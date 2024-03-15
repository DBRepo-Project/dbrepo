<template>
  <div>
    <DatabaseToolbar />
    <v-window v-model="tab">
      <TableList />
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableList from '@/components/table/TableList'
import DatabaseToolbar from '@/components/database/DatabaseToolbar'

export default {
  name: 'Tables',
  components: {
    TableList,
    DatabaseToolbar
  },
  data () {
    return {
      db: null,
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
      ]
    }
  },
  computed: {
    tab () {
      return 1
    }
  }
}
</script>

<style scoped>
a.table_from_csv {
  font-size: 14pt;
}
</style>
