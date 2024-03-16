<template>
  <div v-if="view">
    <ViewToolbar />
    <v-toolbar
      color="secondary"
      :title="$t('toolbars.database.current')"
      flat>
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
import QueryResults from '@/components/subset/Results'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    QueryResults
  },
  data () {
    return {
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'},
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}`
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
          title: this.$t('navigation.data'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/data`,
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
