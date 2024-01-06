<template>
  <div>
    <DatabaseToolbar />
    <v-tabs-items v-model="tab">
      <TableList />
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableList from '@/components/TableList.vue'
import DatabaseToolbar from '@/components/DatabaseToolbar.vue'

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
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}/table`, activeClass: '' }
      ]
    }
  },
  computed: {
    tab () {
      return 1
    }
  },
  mounted () {
  },
  methods: {
  }
}
</script>

<style scoped>
a.table_from_csv {
  font-size: 14pt;
}
</style>
