<template>
  <v-card
    v-if="database"
    :to="`/database/${database.id}/info`"
    variant="flat"
    rounded="0"
    :href="`/database/${database.id}`">
    <v-divider class="mx-4" />
    <v-card-title
      class="text-primary text-decoration-underline"
      v-text="formatTitle(database)" />
    <v-card-subtitle
      v-text="formatCreators(database)" />
    <v-card-text>
      <div v-text="identifierDescription(database)" />
      <div class="mt-2 db-tags">
        <v-chip
          v-if="database.is_public"
          size="small"
          color="success"
          :text="$t('toolbars.database.public')"
          variant="outlined" />
        <v-chip
          v-if="!database.is_public"
          size="small"
          :color="colorVariant"
          :text="$t('toolbars.database.private')"
          flat />
        <v-chip
          v-if="identifierYear(database)"
          size="small"
          :color="colorVariant"
          variant="outlined"
          v-text="identifierYear(database)" />
        <v-chip
          v-if="identifier(database)"
          size="small"
          :color="colorVariant"
          variant="outlined"
          v-text="identifierPublisher(database)" />
        <v-chip
          v-for="(license, i) in identifierLicenses(database)"
          :key="`l-${i}`"
          size="small"
          color="success"
          variant="outlined"
          v-text="license.identifier" />
        <v-chip
          v-for="(funder, i) in identifierFunders(database)"
          :key="`f-${i}`"
          size="small"
          :color="colorVariant"
          variant="outlined"
          v-text="funder.funder_name" />
        <v-chip
          v-if="identifierLanguage(database)"
          size="small"
          :color="colorVariant"
          variant="outlined"
          v-text="identifierLanguage(database)" />
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
import { formatLanguage } from '@/utils'

export default {
  props: {
    database: {
      default: () => {
        return null
      }
    }
  },
  computed: {
    colorVariant () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? '' : 'secondary'
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
      if (!database || !database.identifiers || database.identifiers.length === 0) {
        return null
      }
      return database.identifiers[0]
    },
  }
}
</script>
<style lang="scss" scoped>
.db-tags .v-chip:not(:first-child) {
  margin-left: 4px;
}
</style>
