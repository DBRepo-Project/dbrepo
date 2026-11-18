<template>
  <div>
    <v-toolbar
      variant="flat">
      <v-toolbar-title>
        {{ header }}
      </v-toolbar-title>
    </v-toolbar>
    <v-card
      v-if="!q"
      rounded="0"
      variant="flat">
      <AdvancedSearch
        :key="$route.fullPath"
        ref="adv"
        :loading="loading"
        @clear="clearResults"
        @search="searchResults" />
    </v-card>
    <v-alert v-if="failed" type="error" variant="tonal" role="alert">
      {{ $t('pages.search.load-error') }}
      <v-btn variant="text" @click="searchFromRoute">{{ $t('navigation.retry') }}</v-btn>
    </v-alert>
    <DatabaseList
      v-else-if="isDatabaseSearch"
      :loading="loading"
      :databases="results" />
    <div
      v-else>
      <v-card
        v-for="(result, idx) in results"
        :key="idx"
        :to="link(result) && link(result).startsWith('http') ? null : link(result)"
        :href="link(result) && link(result).startsWith('http') ? link(result): null"
        variant="flat"
        rounded="0">
        <v-divider class="mx-4" />
        <v-card-title
          class="text-primary text-decoration-underline">
          <a
            v-if="link(result)"
            :href="link(result)">
            {{ title(result) }}
          </a>
          <span
            v-else>
            {{ title(result) }}
          </span>
        </v-card-title>
        <v-card-subtitle>
          {{ description(result) }}
        </v-card-subtitle>
        <v-card-text>
          <div
            v-if="tags(result).length > 0"
            class="mt-2 db-tags">
            <ResourceStatus
              v-if="isTable(result) || isView(result)"
              :resource="result" />
            <v-chip
              v-for="(tag, i) in tags(result)"
              :key="i"
              size="small"
              :color="tag.color"
              variant="outlined">
              {{ tag.text }}
            </v-chip>
          </div>
        </v-card-text>
      </v-card>
    </div>
  </div>
</template>

<script>
import AdvancedSearch from '@/components/search/AdvancedSearch.vue'

export default {
  components: {
    AdvancedSearch
  },
  data () {
    return {
      results: [],
      type: 'database',
      loading: true,
      failed: false,
      requestId: 0
    }
  },
  computed: {
    q () {
      const term = this.$route.query.q
      return typeof term === 'string' ? term.trim() : ''
    },
    header () {
      if (this.loading || this.failed) return this.$t('navigation.search')
      return `${this.results.length} ${this.results.length !== 1 ? this.$t('toolbars.search.results') : this.$t('toolbars.search.result')}`
    },
    isDatabaseSearch () {
      return this.type === 'database'
    }
  },
  watch: {
    '$route.fullPath': 'searchFromRoute'
  },
  mounted () {
    this.searchFromRoute()
  },
  methods: {
    async searchFromRoute () {
      if (this.q) return this.searchResults('database')
      await this.$nextTick()
      this.$refs.adv?.advancedSearch()
    },
    clearResults (type) {
      this.requestId++
      this.onSearchResult([], type)
      this.loading = false
      this.failed = false
    },
    async searchResults (type, filters) {
      this.clearResults(type)
      const request = this.requestId
      this.loading = true
      try {
        const searchService = useSearchService()
        const results = this.q ? await searchService.fuzzy_search(this.q) : await searchService.general_search(type, filters)
        if (request === this.requestId) this.onSearchResult(results, type)
      } catch {
        if (request === this.requestId) this.failed = true
      } finally {
        if (request === this.requestId) this.loading = false
      }
    },
    isDatabase (item) {
      if (!item) {
        return false
      }
      if ('exchange_name' in item) {
        return true
      }
      return this.type === 'database'
    },
    isConcept (item) {
      if (!item) {
        return false
      }
      return this.type === 'concept'
    },
    isUnit (item) {
      if (!item) {
        return false
      }
      return this.type === 'unit'
    },
    isTable (item) {
      if (!item) {
        return false
      }
      return this.type === 'table'
    },
    isColumn (item) {
      if (!item) {
        return false
      }
      return this.type === 'column'
    },
    isUser (item) {
      if (!item) {
        return false
      }
      return this.type === 'user'
    },
    isView (item) {
      if (!item) {
        return false
      }
      return this.type === 'view'
    },
    isIdentifier (item) {
      if (!item) {
        return false
      }
      return this.type === 'identifier'
    },
    title (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isColumn(item) || this.isView(item)) {
        return item.name
      } else if (this.isConcept(item) || this.isUnit(item)) {
        return item.uri
      } if (this.isIdentifier(item)) {
        const identifierService = useIdentifierService()
        const title = identifierService.identifierPreferEnglishTitle(item)
        if (!title) {
          return this.$t('pages.identifier.titles.none')
        }
        return title
      } else if (this.isUser(item)) {
        return item.qualified_name || item.username
      }
      return null
    },
    description (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isConcept(item) || this.isUnit(item)) {
        return item.description
      } else if (this.isIdentifier(item)) {
        const identifierService = useIdentifierService()
        const description = identifierService.identifierPreferEnglishDescription(item)
        if (!description) {
          return this.$t('pages.identifier.descriptions.none')
        }
        return description
      } else if (this.isColumn(item)) {
        let text = item.type
        if (item.size) {
          text += `(${item.size}${item.d ? ',' + item.d : ''})`
        }
        return text
      } else if (this.isView(item)) {
        return item.query
      } else if (this.isUser(item)) {
        return item.name
      }
      return null
    },
    link (item) {
      if (this.isDatabase(item)) {
        return `/database/${item.id}/info`
      } else if (this.isTable(item)) {
        return `/database/${item.database_id}/table/${item.id}/info`
      } else if (this.isView(item)) {
        return `/database/${item.database_id}/view/${item.id}/info`
      } else if (this.isColumn(item)) {
        return `/database/${item.database_id}/table/${item.table_id}/schema`
      } else if (this.isIdentifier(item)) {
        return `/pid/${item.id}`
      } else if (this.isConcept(item) || this.isUnit(item)) {
        return item.uri
      }
      return null
    },
    tags (item) {
      const tags = []
      if (this.isDatabase(item)) {
      } else if (this.isTable(item)) {
      } else if (this.isColumn(item)) {
        if (item.concept_uri || item.concept) {
          const conceptName = item.concept_uri || item.concept.name || item.concept.uri
          tags.push({ color: 'success', text: conceptName })
        }
        if (item.unit_uri || item.unit) {
          const unitName = item.unit_uri || item.unit.name || item.unit.uri
          tags.push({ color: 'success', text: unitName })
        }
      } else if (this.isView(item)) {
      } else if (this.isIdentifier(item)) {
        if (item.publication_year) {
          tags.push({ text: item.publication_year })
        }
        if (item.publisher) {
          tags.push({ text: item.publisher })
        }
        if (item.licenses) {
          item.licenses.forEach(l => tags.push({text: l.identifier, color: 'success'}))
        }
        if (item.funders) {
          item.funders.forEach(f => tags.push({text: f.funder_name}))
        }
        if (item.language) {
          tags.push({ text: item.language })
        }
        tags.push({ text: this.capitalizeFirstLetter(item.status), color: item.status === 'published' ? 'success' : null })
      } else if (this.isUnit(item)) {
      } else if (this.isConcept(item)) {
      } else if (this.isUser(item)) {
        if (item.orcid) {
          tags.push({ text: 'ORCID', color: 'success' })
        }
      }
      return tags
    },
    onSearchResult (results, type) {
      this.type = type
      this.results = results
    },
    capitalizeFirstLetter(string) {
      if (!string) {
        return
      }
      return string.charAt(0).toUpperCase() + string.slice(1);
    }
  }
}
</script>
<style lang="scss" scoped>
.db-tags .v-chip:not(:first-child) {
  margin-left: 4px;
}
</style>
