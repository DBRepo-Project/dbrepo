<template>
  <div>
    <v-toolbar flat tile>
      <v-toolbar-title v-text="header" />
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canCreateDatabase" color="primary" name="create-database" @click.stop="createDbDialog = true">
          <v-icon left>mdi-plus</v-icon> Database
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat tile>
      <AdvancedSearch ref="adv" @search-result="onSearchResult" />
    </v-card>
    <DatabaseList v-if="isDatabaseSearch" :databases="results.results" />
    <div v-else>
      <v-card
        v-for="(result, idx) in results.results"
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
        <v-card-subtitle class="db-subtitle" v-text="description(result)" />
        <v-card-text class="db-description">
          <div v-if="tags(result).length > 0" class="db-tags">
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
    <v-dialog
      v-model="createDbDialog"
      persistent
      max-width="640">
      <CreateDB @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import CreateDB from '@/components/dialogs/CreateDB'
import SearchService from '@/api/search.service'
import AdvancedSearch from '@/components/search/AdvancedSearch'
import IdentifierMapper from '@/api/identifier.mapper'

export default {
  components: {
    CreateDB,
    AdvancedSearch
  },
  data () {
    return {
      results: {
        results: [],
        type: null
      },
      loading: false,
      createDbDialog: null
    }
  },
  computed: {
    roles () {
      return this.$store.state.roles
    },
    query () {
      if (!this.$route.query || !this.$route.query.q) {
        return null
      }
      return this.$route.query.q
    },
    header () {
      if (!this.results || !this.results.results) {
        return '0 results'
      }
      if (this.results.results.length !== 1) {
        return `${this.results.results.length} results`
      }
      return `${this.results.results.length} result`
    },
    canCreateDatabase () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database')
    },
    isDatabaseSearch () {
      return this.results.type === 'database'
    }
  },
  watch: {
    $route: {
      handler () {
        this.generalSearch()
      }
    }
  },
  mounted () {
    if (this.query) {
      this.generalSearch()
    }
  },
  methods: {
    generalSearch () {
      if (this.loading) {
        return
      }
      this.loading = true
      SearchService.search(null, { search_term: this.query })
        .then((response) => {
          this.results = response
        })
        .finally(() => {
          this.loading = false
        })
    },
    isDatabase (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'database'
    },
    isConcept (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'concept'
    },
    isUnit (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'unit'
    },
    isTable (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'table'
    },
    isColumn (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'column'
    },
    isUser (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'user'
    },
    isView (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'view'
    },
    isIdentifier (item) {
      if (!item) {
        return false
      }
      return this.results.type === 'identifier'
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
        return IdentifierMapper.identifierPreferEnglishTitle(item)
      } else if (this.isUser(item)) {
        return item.creator.qualified_name
      }
      return null
    },
    description (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isConcept(item) || this.isUnit(item)) {
        return item.description
      } else if (this.isIdentifier(item)) {
        return IdentifierMapper.identifierPreferEnglishDescription(item)
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
        return `/database/${item.id}`
      } else if (this.isTable(item)) {
        return `/database/${item.database_id}/table/${item.id}`
      } else if (this.isView(item)) {
        return `/database/${item.database_id}/view/${item.id}`
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
        tags.push({ color: this.isPublic(item) ? 'green' : null, text: this.isPublic(item) ? 'Public' : 'Private' })
      }
      if (this.isDatabase(item)) {
      } else if (this.isTable(item)) {
      } else if (this.isColumn(item)) {
        if ('concept' in item) {
          const conceptName = ('name' in item.concept) ? item.concept.name : 'Concept'
          tags.push({ color: 'green', text: conceptName })
        }
        if ('unit' in item) {
          const unitName = ('name' in item.unit) ? item.unit.name : 'Unit'
          tags.push({ color: 'green', text: unitName })
        }
      } else if (this.isView(item)) {
      } else if (this.isIdentifier(item)) {
      } else if (this.isUnit(item)) {
      } else if (this.isConcept(item)) {
      } else if (this.isUser(item)) {
        if (item.creator.attributes.orcid) {
          tags.push({ text: 'ORCID', color: 'green' })
        }
      }
      return tags
    },
    closed (event) {
      this.createDbDialog = false
      if (event.success) {
        this.$router.push('/database?f=my')
      }
    },
    onSearchResult (results) {
      console.debug('found search results', results)
      this.results = results
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
