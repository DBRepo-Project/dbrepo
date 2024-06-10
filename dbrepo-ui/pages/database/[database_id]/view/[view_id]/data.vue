<template>
  <div>
    <ViewToolbar
      v-if="view" />
    <v-toolbar
      color="secondary"
      :title="$t('toolbars.database.current')"
      flat>
      <v-btn
        :prepend-icon="$vuetify.display.lgAndUp ? 'mdi-refresh' : null"
        variant="flat"
        :text="$t('toolbars.table.data.refresh')"
        class="mb-1 ml-2"
        :loading="loadingData"
        @click="reload" />
    </v-toolbar>
    <TimeDrift />
    <v-card tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
        type="view"
        class="mt-0 mb-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import TimeDrift from '@/components/TimeDrift.vue'
import QueryResults from '@/components/subset/Results.vue'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    QueryResults,
    TimeDrift
  },
  data () {
    return {
      loadingData: false,
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
    this.reload()
  },
  methods: {
    reload () {
      this.$refs.queryResults.reExecute(Number(this.$route.params.view_id))
      this.$refs.queryResults.reExecuteCount(Number(this.$route.params.view_id))
    }
  }
}
</script>
