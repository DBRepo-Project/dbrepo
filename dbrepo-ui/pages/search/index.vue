<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title v-text="header" />
    </v-toolbar>
    <v-progress-linear v-if="loading" color="primary" />
    <v-card
      v-for="(result, idx) in results"
      :key="idx"
      :to="link(result)"
      flat
      tile>
      <v-divider class="mx-4" />
      <v-card-title>
        <a :href="link(result)">{{ title(result) }}</a>
      </v-card-title>
      <v-card-subtitle class="search-subtitle" v-text="description(result)" />
      <v-card-text class="search-description">
        <div class="search-tags">
          <v-chip v-if="isPublic(result) === true" small color="green" outlined>Public</v-chip>
          <v-chip v-if="isPublic(result) === false" small color="red" outlined>Private</v-chip>
          <v-chip v-if="isTable(result)" small outlined>Table</v-chip>
          <v-chip v-if="isColumn(result)" small outlined>Column</v-chip>
          <v-chip v-if="isView(result)" small outlined>View</v-chip>
          <v-chip v-if="isIdentifier(result)" small outlined>Identifier</v-chip>
          <v-chip v-if="isDatabase(result) || (isIdentifier(result) && result.type === 'DATABASE')" small outlined>Database</v-chip>
          <v-chip v-if="isIdentifier(result) && result.type === 'SUBSET'" small outlined>Subset</v-chip>
          <v-chip v-if="isIdentifier(result) && result.publicationYear" small outlined>{{ result.publicationYear }}</v-chip>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import AdvancedSearchService from '@/api/advanced_search.service'
import EventBus from '@/api/eventBus'
import SearchService from '@/api/search.service'

export default {
  inject: ['advancedSearchData'],
  data () {
    return {
      results: [],
      loading: false
    }
  },
  computed: {
    query () {
      if (!this.$route.query || !this.$route.query.q) {
        return null
      }
      return this.$route.query.q
    },
    type () {
      if (!this.$route.query || !this.$route.query.t) {
        return null
      }
      return this.$route.query.t
    },
    header () {
      if (this.results.length !== 1) {
        return `${this.results.length} results`
      }
      return `${this.results.length} result`
    }
  },
  watch: {
    '$route.query.q': {
      handler (query) {
        if (this.advancedSearchData) {
          return
        }
        this.retrieve()
      },
      deep: true,
      immediate: true
    },
    '$route.query.t': {
      handler (type) {
        if (this.advancedSearchData) {
          return
        }
        this.retrieve()
      },
      deep: true,
      immediate: true
    }
  },
  created () {
    EventBus.$on('advancedSearchButtonClicked', () => {
      this.doAdvancedSearch(this.advancedSearchData)
    })
  },
  beforeDestroy () {
    EventBus.$off('advancedSearchButtonClicked')
  },
  mounted () {
    if (Object.keys(this.advancedSearchData).some(key => key !== 'search_term')) {
      this.doAdvancedSearch(this.advancedSearchData)
    } else if (this.query) {
      this.retrieve(this.query)
    }
  },
  methods: {
    retrieve () {
      if (this.loading) {
        return
      }
      this.loading = true
      SearchService.search(this.query)
        .then((hits) => {
          this.results = hits.map(h => h._source)
        })
        .finally(() => {
          this.loading = false
        })
    },
    doAdvancedSearch (advancedSearchData) {
      console.log('Advanced Search Data:', advancedSearchData)
      AdvancedSearchService.search(advancedSearchData)
        .then((response) => {
          const hits = response.hits.hits
          this.results = hits.map(h => h._source)
          console.log('Advanced Search Results', this.results)
        })
        .finally(() => {
          this.loading = false
        })
    },
    isDatabase (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.database.DatabaseDto/.test(item._class)
      }
      return item.exchangeName !== undefined
    },
    isConcept (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.database.table.columns.concepts.ConceptDto/.test(item._class)
      }
      return false
    },
    isUnit (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.database.table.columns.concepts.UnitDto/.test(item._class)
      }
      return false
    },
    isTable (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.database.table.TableDto/.test(item._class)
      }
      return false
    },
    isColumn (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.entities.database.table.columns.TableColumn/.test(item._class)
      }
      return false
    },
    isView (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.database.ViewDto/.test(item._class)
      }
      return false
    },
    isIdentifier (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.identifier.IdentifierDto/.test(item._class)
      }
      return false
    },
    isPublic (item) {
      if (this.isDatabase(item)) {
        return item.isPublic
      } else if (this.isTable(item)) {
        return item.isPublic
      } else if (this.isColumn(item)) {
        return item.isPublic
      } else if (this.isView(item)) {
        return item.isPublic
      } else if (this.isIdentifier(item)) {
        return item.visibility === 'EVERYONE'
      }
      return false
    },
    title (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isColumn(item) || this.isView(item) || this.isConcept(item) || this.isUnit(item)) {
        return item.name
      } else if (this.isIdentifier(item)) {
        return item.title
      }
      return null
    },
    description (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isIdentifier(item) || this.isConcept(item) || this.isUnit(item)) {
        return item.description
      } else if (this.isColumn(item)) {
        return null
      } else if (this.isView(item)) {
        return item.query
      }
      return false
    },
    link (item) {
      if (this.isDatabase(item)) {
        return `/database/${item.id}`
      }
      if (this.isTable(item)) {
        return `/database/${item.databaseId}/table/${item.id}`
      }
      if (this.isView(item)) {
        return `/database/${item.vdbid}/view/${item.id}`
      }
      if (this.isColumn(item)) {
        return `/database/${item.cdbid}/table/${item.tid}`
      }
      if (this.isIdentifier(item)) {
        return `/pid/${item.id}`
      }
      return '/'
    }
  }
}
</script>
<style>
.search-subtitle {
  padding-bottom: 8px;
}
.search-tags {
  margin-bottom: 8px;
}
</style>
