<template>
  <v-card
    v-if="database"
    :to="`/database/${database.id}/info`"
    variant="flat"
    rounded="0"
    :href="`/database/${database.id}`"
    @click="loading = true">
    <v-divider class="mx-4" />
    <v-card-title>
      <span
        class="text-primary text-decoration-underline">
        {{ formatTitle(database) }}
      </span>
      <v-progress-circular
        v-if="loading"
        color="primary"
        size="24"
        class="ml-1"
        indeterminate />
    </v-card-title>
    <v-card-subtitle>
      {{ formatCreators(database) }}
    </v-card-subtitle>
    <v-card-text>
      <div>
        {{ identifierDescription(database) }}
      </div>
      <div class="mt-2 db-tags">
        <v-chip
          v-if="database.is_public"
          size="small"
          color="success"
          variant="outlined">
          {{ $t('toolbars.database.public') }}
        </v-chip>
        <v-chip
          v-if="!database.is_public"
          size="small"
          :color="colorVariant"
          variant="outlined"
          flat>
          {{ $t('toolbars.database.private') }}
        </v-chip>
        <v-chip
          v-if="identifierYear(database)"
          size="small"
          :color="colorVariant"
          variant="outlined">
          {{ identifierYear(database) }}
        </v-chip>
        <v-chip
          v-if="identifier(database)"
          size="small"
          :color="colorVariant"
          variant="outlined">
          {{ identifierPublisher(database) }}
        </v-chip>
        <v-chip
          v-for="(license, i) in identifierLicenses(database)"
          :key="`l-${i}`"
          size="small"
          color="success"
          variant="outlined">
          {{ license.identifier }}
        </v-chip>
        <v-chip
          v-for="(funder, i) in identifierFunders(database)"
          :key="`f-${i}`"
          size="small"
          :color="colorVariant"
          variant="outlined">
          {{ funder.funder_name }}
        </v-chip>
        <v-chip
          v-if="identifierLanguage(database)"
          size="small"
          :color="colorVariant"
          variant="outlined">
          {{ identifierLanguage(database) }}
        </v-chip>
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
import { formatLanguage } from '@/utils'

export default {
  data() {
    return {
      loading: false
    }
  },
  props: {
    database: {
      default: () => {
        return null
      }
    }
  },
  computed: {
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
    },
    isContrastTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast')
    },
    isDarkTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().startsWith('dark')
    },
    identifiers () {
      if (!this.database || !this.database.identifiers) {
        return []
      }
      return this.database.identifiers.filter(i => i.status === 'published')
    }
  },
  methods: {
    formatCreators (database) {
      if (!this.identifier(database)) {
        const databaseService = useDatabaseService()
        return databaseService.databaseToOwner(database)
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierToCreators(this.identifier(database))
    },
    formatTitle (database) {
      if (!this.identifier(database)) {
        return database.name
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierPreferEnglishTitle(this.identifier(database))
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
      const identifierService = useIdentifierService()
      return identifierService.descriptionShort(identifierService.identifierPreferEnglishDescription(this.identifier(database)))
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
      if (!database || !this.identifiers || this.identifiers.length === 0) {
        return null
      }
      return this.identifiers[0]
    },
  }
}
</script>
<style lang="scss" scoped>
.db-tags .v-chip:not(:first-child) {
  margin-left: 4px;
}
</style>
