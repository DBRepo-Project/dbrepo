<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title v-text="header" />
    </v-toolbar>
    <v-progress-linear v-if="loading" color="primary" />
    <v-card
      v-for="(result, idx) in results"
      :key="idx"
      :to="link(result) && link(result).startsWith('http') ? null : link(result)"
      :href="link(result) && link(result).startsWith('http') ? link(result): null"
      flat
      tile>
      <v-divider class="mx-4" />
      <v-card-title>
        <a v-if="link(result)" :href="link(result)">{{ title(result) }}</a>
        <span v-else>{{ title(result) }}</span>
      </v-card-title>
      <v-card-subtitle class="search-subtitle" v-text="description(result)" />
      <v-card-text v-if="tags(result).length > 0" class="search-description">
        <div class="search-tags">
          <v-chip
            v-for="(tag, i) in tags(result)"
            :key="i"
            small
            :color="tag.color"
            outlined
            v-text="tag.text" />
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import EventBus from '@/api/eventBus'
import SearchService from '@/api/search.service'

export default {
  inject: ['advancedSearchData', 'advancedSearchType'],
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
      this.doAdvancedSearch(this.advancedSearchType, this.advancedSearchData)
    })
  },
  beforeDestroy () {
    EventBus.$off('advancedSearchButtonClicked')
  },
  mounted () {
    if (Object.keys(this.advancedSearchData).some(key => key !== 'search_term')) {
      this.doAdvancedSearch(this.advancedSearchType, this.advancedSearchData)
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
      SearchService.search(this.type, this.query, [])
        .then((hits) => {
          this.results = hits.map(h => h._source)
        })
        .finally(() => {
          this.loading = false
        })
    },
    doAdvancedSearch (advancedSearchType, advancedSearchData) {
      console.debug('advanced search type:', advancedSearchType, 'data:', advancedSearchData)
      SearchService.search(advancedSearchType, null, advancedSearchData)
        .then((response) => {
          this.results = response.map(h => h._source)
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
        return /at.tuwien.api.database.table.columns.ColumnDto/.test(item._class)
      }
      return false
    },
    isUser (item) {
      if (!item) {
        return false
      }
      if ('_class' in item) {
        return /at.tuwien.api.user.UserDto/.test(item._class)
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
      if (this.isDatabase(item) || this.isTable(item) || this.isColumn(item) || this.isView(item)) {
        return item.is_public
      } else if (this.isIdentifier(item)) {
        return item.visibility === 'EVERYONE'
      }
      return null
    },
    title (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isColumn(item) || this.isView(item) || this.isConcept(item) || this.isUnit(item)) {
        return item.name
      } else if (this.isIdentifier(item)) {
        return item.title
      } else if (this.isUser(item)) {
        return item.username
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
      return null
    },
    link (item) {
      if (this.isDatabase(item)) {
        return `/database/${item.id}`
      } else if (this.isTable(item)) {
        return `/database/${item.databaseId}/table/${item.id}`
      } else if (this.isView(item)) {
        return `/database/${item.vdbid}/view/${item.id}`
      } else if (this.isColumn(item)) {
        return `/database/${item.cdbid}/table/${item.tid}`
      } else if (this.isIdentifier(item)) {
        return `/pid/${item.id}`
      } else if (this.isConcept(item) || this.isUnit(item)) {
        return item.uri
      }
      return null
    },
    tags (item) {
      const tags = []
      if (this.isPublic(item) === true || this.isPublic(item) === false) {
        tags.push({ color: this.isPublic(item) ? 'green' : 'red', text: this.isPublic(item) ? 'Public' : 'Private' })
      }
      if (this.isDatabase(item)) {
        tags.push({ text: 'Database' })
      } else if (this.isTable(item)) {
        tags.push({ text: 'Table' })
      } else if (this.isColumn(item)) {
        tags.push({ text: 'Column' })
        if ('concept' in item) {
          const conceptName = ('name' in item.concept) ? item.concept.name : 'Concept'
          tags.push({ color: 'green', text: conceptName })
        }
        if ('unit' in item) {
          const unitName = ('name' in item.unit) ? item.unit.name : 'Unit'
          tags.push({ color: 'green', text: unitName })
        }
      } else if (this.isView(item)) {
        tags.push({ text: 'View' })
      } else if (this.isIdentifier(item)) {
        tags.push({ text: 'Identifier' })
      } else if (this.isUnit(item)) {
        tags.push({ text: 'Unit' })
      } else if (this.isConcept(item)) {
        tags.push({ text: 'Concept' })
      }
      return tags
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
