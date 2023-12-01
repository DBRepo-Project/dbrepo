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
    <v-dialog
      v-model="createDbDialog"
      persistent
      max-width="640">
      <CreateDB @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import SearchService from '@/api/search.service'
import CreateDB from '@/components/dialogs/CreateDB'
import AdvancedSearch from '@/components/search/AdvancedSearch'
import IdentifierMapper from '@/api/identifier.mapper'

export default {
  components: {
    CreateDB,
    AdvancedSearch
  },
  data () {
    return {
      results: [],
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
    },
    canCreateDatabase () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database')
    }
  },
  watch: {
    '$route.query.q': {
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
        .then((hits) => {
          this.results = hits.map(h => h._source)
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
        return IdentifierMapper.identifierPreferEnglishTitle(item)
      } else if (this.isUser(item)) {
        return item.username
      }
      return null
    },
    description (item) {
      if (this.isDatabase(item) || this.isTable(item) || this.isConcept(item) || this.isUnit(item)) {
        return item.description
      } else if (this.isIdentifier(item)) {
        return IdentifierMapper.identifierPreferEnglishDescription(item)
      } else if (this.isColumn(item)) {
        return null
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
        return `/database/${item.database_id}/table/${item.table_id}`
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
      } else if (this.isUser(item)) {
        tags.push({ text: 'User' })
        if ('orcid' in item.attributes && item.attributes.orcid) {
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
