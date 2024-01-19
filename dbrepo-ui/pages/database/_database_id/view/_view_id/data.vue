<template>
  <div v-if="view">
    <ViewToolbar />
    <v-toolbar color="secondary white--text" flat>
      <v-toolbar-title>
        <strong>Current</strong>
      </v-toolbar-title>
    </v-toolbar>
    <v-card tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
        type="view"
        :view="view"
        class="mt-0 mb-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import QueryResults from '@/components/query/Results.vue'
export default {
  components: {
    QueryResults
  },
  data () {
    return {
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Views', to: `/database/${this.$route.params.database_id}/view`, activeClass: '' },
        { text: `${this.$route.params.view_id}`, to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`, activeClass: '' },
        { text: 'Data', to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/data`, activeClass: '' }
      ]
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    }
  },
  mounted () {
    if (!this.view) {
      return
    }
    this.$refs.queryResults.reExecute(this.view.id)
    this.$refs.queryResults.reExecuteCount(this.view.id)
  }
}
</script>
<style>
</style>
