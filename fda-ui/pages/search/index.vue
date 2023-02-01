<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title v-text="header" />
    </v-toolbar>
    <v-progress-linear v-if="loading" color="primary" />
    <v-card
      v-for="(result, idx) in results"
      :key="idx"
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
export default {
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
        this.retrieve(query)
      },
      deep: true,
      immediate: true
    }
  },
  mounted () {
    if (this.query) {
      this.retrieve(this.query)
    }
  },
  methods: {
    async retrieve (v) {
      if (this.loading) {
        return
      }
      this.loading = true
      try {
        const res = await this.$axios.get(`/retrieve/databaseindex,tableindex,columnindex,identifierindex,viewindex/_search?q=${v}*&terminate_after=50`)
        console.info('search results', res.data.hits.total.value)
        console.debug('search results for', this.$route.query.q, 'are', res.data.hits.hits)
        this.results = res.data.hits.hits.map(h => h._source)
      } catch (err) {
        console.error('Failed to load search results', err)
      }
      this.loading = false
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
      if (this.isDatabase(item) || this.isTable(item) || this.isColumn(item) || this.isView(item)) {
        return item.name
      } else if (this.isIdentifier(item)) {
        return item.title
      }
      return null
    },
    description (item) {
      if (this.isDatabase(item)) {
        return item.description
      } else if (this.isTable(item)) {
        return item.description
      } else if (this.isColumn(item)) {
        return null
      } else if (this.isView(item)) {
        return item.query
      } else if (this.isIdentifier(item)) {
        return item.description
      }
      return false
    },
    link (item) {
      if (this.isDatabase(item)) {
        return `/container/${item.id}/database/${item.id}`
      }
      if (this.isTable(item)) {
        return `/container/${item.tdbid}/database/${item.tdbid}/table/${item.id}`
      }
      if (this.isView(item)) {
        return `/container/${item.vdbid}/database/${item.vdbid}/view/${item.id}`
      }
      if (this.isColumn(item)) {
        return `/container/${item.cdbid}/database/${item.cdbid}/table/${item.tid}`
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
