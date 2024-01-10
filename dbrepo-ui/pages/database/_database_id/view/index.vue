<template>
  <div>
    <DatabaseToolbar />
    <v-tabs-items v-model="tab">
      <ViewList />
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import ViewList from '@/components/view/ViewList.vue'

export default {
  name: 'Views',
  components: {
    ViewList,
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
</style>
