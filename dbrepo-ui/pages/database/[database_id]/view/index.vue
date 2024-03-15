<template>
  <div>
    <DatabaseToolbar />
    <v-window v-model="tab">
      <ViewList />
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar'
import ViewList from '@/components/view/ViewList'

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
