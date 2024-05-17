<template>
  <div>
    <v-toolbar
      variant="flat">
      <v-toolbar-title
        v-text="header" />
      <v-spacer />
      <v-btn
        v-if="canCreateDatabase"
        class="mr-4"
        prepend-icon="mdi-plus"
        :text="$t('toolbars.database.create.text')"
        color="primary"
        variant="flat"
        @click.stop="createDbDialog = true" />
    </v-toolbar>
    <v-card
      rounded="0"
      variant="flat">
      <AdvancedSearch
        ref="adv"
        @search-result="onSearchResult" />
    </v-card>
    <DatabaseList
      v-if="isDatabaseSearch"
      :loading="loading"
      :databases="results" />
    <div v-else>
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
          <a v-if="link(result)" :href="link(result)">{{ title(result) }}</a>
          <span v-else>{{ title(result) }}</span>
        </v-card-title>
        <v-card-subtitle v-text="description(result)" />
        <v-card-text>
          <div
            v-if="tags(result).length > 0"
            class="mt-2 db-tags">
            <v-chip
              v-for="(tag, i) in tags(result)"
              :key="i"
              size="small"
              :color="tag.color"
              variant="outlined"
              v-text="tag.text" />
          </div>
        </v-card-text>
      </v-card>
    </div>
    <v-dialog
      v-model="createDbDialog"
      persistent
      max-width="640">
      <DatabaseCreate @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import DatabaseCreate from '@/components/database/DatabaseCreate.vue'
import AdvancedSearch from '@/components/search/AdvancedSearch.vue'
import { useUserStore } from '@/stores/user'

export default {
  components: {
    DatabaseCreate,
    AdvancedSearch
  },
  data () {
    return {
      results: [],
      type: 'database',
      loading: false,
      createDbDialog: null,
      userStore: useUserStore()
    }
  },
  computed: {
    roles () {
      return this.userStore.getRoles
    },
    q () {
      if (!this.$route.query || !this.$route.query.q) {
        return null
      }
      return this.$route.query.q
    },
    header () {
      return `${this.results.length} ${this.results.length !== 1 ? this.$t('toolbars.search.results') : this.$t('toolbars.search.result')}`
    },
    canCreateDatabase () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database')
    },
    isDatabaseSearch () {
      return this.type === 'database'
    }
  },
  watch: {
    $route: {
      handler () {
        this.fuzzySearch()
      }
    }
  },
  mounted () {
    this.fuzzySearch()
  },
  methods: {
    fuzzySearch () {
      if (this.loading) {
        return
      }
      const queryKeys = Object.keys(this.$route.query)
      if (!queryKeys || queryKeys.length !== 1 || !queryKeys.includes('q')) {
        return
      }
      this.loading = true
      const searchService = useSearchService()
      searchService.fuzzy_search(this.q)
        .then(({results}) => {
          this.results = results
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
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
    isPublic (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isColumn(item) || this.isView(item) || this.isIdentifier(item)) {
        return item.is_public
      }
      return null
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
        return item.creator.qualified_name
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
        let text = item.column_type
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
      if (this.isPublic(item) === true || this.isPublic(item) === false) {
        const text = this.isPublic(item) ? this.$t('toolbars.database.public') : this.$t('toolbars.database.private')
        tags.push({ color: this.isPublic(item) ? 'success' : null, text })
      }
      if (this.isDatabase(item)) {
      } else if (this.isTable(item)) {
      } else if (this.isColumn(item)) {
        if ('concept' in item) {
          const conceptName = ('name' in item.concept) ? item.concept.name : this.$t('pages.search.concept')
          tags.push({ color: 'success', text: conceptName })
        }
        if ('unit' in item) {
          const unitName = ('name' in item.unit) ? item.unit.name : this.$t('pages.search.unit')
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
        if (item.creator.attributes.orcid) {
          tags.push({ text: 'ORCID', color: 'success' })
        }
      }
      return tags
    },
    closed (event) {
      this.dialog = false
      if (event.success) {
        this.$router.push(`/database/${event.database_id}/info`)
      }
    },
    onSearchResult ({results, type}) {
      this.results = results
      if (!type) {
        return
      }
      console.debug('search for type', type, ':', results)
      this.type = type
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
