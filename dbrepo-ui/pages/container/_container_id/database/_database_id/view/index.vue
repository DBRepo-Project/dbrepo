<template>
  <div>
    <DBToolbar />
    <v-tabs-items v-model="tab">
      <ViewList />
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import DBToolbar from '@/components/DBToolbar'
import ViewList from '@/components/ViewList'

export default {
  name: 'Views',
  components: {
    ViewList,
    DBToolbar
  },
  data () {
    return {
      db: null,
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, activeClass: '' }
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
