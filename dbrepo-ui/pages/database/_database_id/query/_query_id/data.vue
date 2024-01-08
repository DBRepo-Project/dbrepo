<template>
  <div>
    <SubsetToolbar />
    <v-toolbar color="secondary white--text" flat>
      <v-toolbar-title>
        <strong v-text="executionUTC" />
      </v-toolbar-title>
    </v-toolbar>
    <v-card tile>
      <QueryResults
        id="query-results"
        ref="queryResults"
        v-model="subset.id"
        type="query"
        class="mt-0 mb-0" />
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import QueryResults from '@/components/query/Results'
import QueryService from '@/api/query.service'
import { formatTimestampUTCLabel } from '@/utils'

export default {
  components: {
    QueryResults
  },
  data () {
    return {
      loadingSubset: false,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Subsets', to: `/database/${this.$route.params.database_id}/query`, activeClass: '' },
        { text: `${this.$route.params.query_id}`, to: `/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, activeClass: '' },
        { text: 'Data', to: `/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}/data`, activeClass: '' }
      ],
      subset: {
        id: this.$route.params.query_id
      }
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
    }
  },
  mounted () {
    this.loadSubset()
  },
  methods: {
    loadSubset () {
      this.loadingSubset = true
      QueryService.findOne(this.$route.params.database_id, this.$route.params.query_id)
        .then((subset) => {
          this.subset = subset
          this.loadResult()
        })
        .catch(() => {
          this.loadingSubset = false
        })
        .finally(() => {
          this.loadingSubset = false
        })
    },
    loadResult () {
      this.$refs.queryResults.reExecute(this.subset.id)
      this.$refs.queryResults.reExecuteCount(this.subset.id)
    }
  }
}
</script>
<style>
</style>
