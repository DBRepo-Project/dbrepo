<template>
  <div>
    <v-card v-if="!$vuetify.theme.dark && databases.length> 0" flat tile>
      <v-divider class="mx-4" />
    </v-card>
    <div v-if="loading">
      <v-card v-for="(idx) in [1,2,3]" :key="idx" flat tile>
        <v-divider class="mx-4" />
        <v-card-subtitle class="db-subtitle">
          <v-skeleton-loader type="text" :style="randomWidth(100,300)" />
          <v-skeleton-loader type="text" class="pt-2" :style="randomWidth(100,200)" />
        </v-card-subtitle>
        <v-card-text class="db-description">
          <v-skeleton-loader type="chip" />
          <v-skeleton-loader type="text" class="pt-4" :style="randomWidth(800,1000)" />
          <v-skeleton-loader type="text" :style="randomWidth(800,1000)" />
          <v-skeleton-loader type="text" :style="randomWidth(600,1000)" />
        </v-card-text>
      </v-card>
    </div>
    <v-card
      v-for="(database, idx) in databases"
      :key="idx"
      :to="`/database/${database.id}/info`"
      flat
      tile>
      <v-divider v-if="idx !== 0" class="mx-4" />
      <v-card-title>
        <a :href="`/database/${database.id}`" v-text="formatTitle(database)" />
      </v-card-title>
      <v-card-subtitle class="db-subtitle" v-text="formatCreators(database)" />
      <v-card-text class="db-description">
        <div class="db-tags">
          <v-chip
            v-if="database.is_public"
            small
            color="success"
            outlined>
            Public
          </v-chip>
          <v-chip v-if="!database.is_public" small outlined>Private</v-chip>
          <v-chip
            v-if="identifierYear(database)"
            small
            outlined
            v-text="identifierYear(database)" />
          <v-chip
            v-if="identifier(database)"
            small
            outlined
            v-text="identifierPublisher(database)" />
          <v-chip
            v-for="(license,i) in identifierLicenses(database)"
            :key="i"
            small
            color="success"
            outlined
            v-text="license.identifier" />
          <v-chip v-for="(funder,i) in identifierFunders(database)" :key="`f-${i}`" small outlined v-text="funder.funder_name" />
          <v-chip v-if="identifierLanguage(database)" small outlined v-text="identifierLanguage(database)" />
        </div>
        <div v-text="identifierDescription(database)" />
      </v-card-text>
    </v-card>
    <v-toolbar v-if="false" flat>
      <v-toolbar-title>
        <v-btn
          small
          color="secondary">
          More
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
  </div>
</template>

<script>
import DatabaseMapper from '@/api/database.mapper'
import IdentifierMapper from '@/api/identifier.mapper'
import { formatLanguage } from '@/utils'

export default {
  props: {
    databases: {
      type: Array,
      default: () => {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: () => {
        return true
      }
    }
  },
  data () {
    return {
      loadingCreate: false,
      createDbDialog: false,
      searchQuery: null,
      limit: 100,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' }
      ],
      error: false
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    }
  },
  methods: {
    formatCreators (database) {
      if (!this.identifier(database)) {
        return DatabaseMapper.databaseToOwner(database)
      }
      return IdentifierMapper.identifierToCreators(this.identifier(database))
    },
    formatTitle (database) {
      if (!this.identifier(database)) {
        return database.name
      }
      return IdentifierMapper.identifierPreferEnglishTitle(this.identifier(database))
    },
    identifierYear (database) {
      if (!this.identifier(database)) {
        return null
      }
      return this.identifier(database).publication_year
    },
    identifierPublisher (database) {
      if (!this.identifier(database)) {
        return null
      }
      return this.identifier(database).publisher
    },
    identifierLicenses (database) {
      if (!this.identifier(database)) {
        return []
      }
      return this.identifier(database).licenses
    },
    identifierDescription (database) {
      if (!this.identifier(database)) {
        return null
      }
      return IdentifierMapper.descriptionShort(IdentifierMapper.identifierPreferEnglishDescription(this.identifier(database)))
    },
    identifierLanguage (database) {
      if (!this.identifier(database) || !this.identifier(database).language) {
        return null
      }
      return formatLanguage(this.identifier(database).language.toLowerCase())
    },
    identifierFunders (database) {
      if (!this.identifier(database)) {
        return null
      }
      return this.identifier(database).funders
    },
    identifier (database) {
      if (!database || !database.identifiers || database.identifiers.length === 0) {
        return null
      }
      return database.identifiers[0]
    },
    formatLanguage,
    randomWidth (min, max) {
      const width = Math.random() * (max - min) + min
      return `width: ${width}px !important;`
    }
  }
}
</script>
<style>
.v-chip:not(:first-child) {
  margin-left: 8px;
}
.db-subtitle {
  padding-bottom: 8px;
}
.db-tags {
  margin-bottom: 8px;
}
.skeleton-small > div {
  width: 100px !important;
}
</style>
