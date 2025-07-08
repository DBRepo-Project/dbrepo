<template>
  <div>
    <v-card
      variant="flat"
      rounded="0"
      v-if="tables.length === 0"
      :text="$t('pages.database.subpages.tables.empty')" />
    <v-card
      variant="flat"
      rounded="0"
      v-for="(table, i) in tables"
      :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list>
        <v-list-item
          lines="two"
          :title="table.name"
          :class="clazz(table)"
          :subtitle="subtitle(table)"
          :to="`/database/${$route.params.database_id}/table/${table.id}/info`">
          <template v-slot:append>
            <ResourceStatus
              :resource="table" />
          </template>
        </v-list-item>
      </v-list>
    </v-card>
  </div>
</template>

<script>
import { formatTimestampUTCLabel, sizeToHumanLabel } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      error: false,
      panel: null,
      column: null,
      dialogSemantic: false,
      mode: 'unit',
      dialogDelete: false,
      headers: [
        { value: 'name', title: 'Name' },
        { value: 'type', title: 'Type' },
        { value: 'column_concept', title: 'Concept' },
        { value: 'column_unit', title: 'Unit' },
        { value: 'is_primary_key', title: 'Primary Key' },
        { value: 'unique', title: 'Unique' },
        { value: 'is_null_allowed', title: 'Nullable' },
      ],
      columnTypes: [
        // { value: 'ENUM', text: 'Enumeration' }, // Disabled for now, not implemented, #145
        { value: 'boolean', title: 'Boolean' },
        { value: 'number', title: 'Number' },
        { value: 'blob', title: 'Binary Large Object' },
        { value: 'date', title: 'Date' },
        { value: 'timestamp', title: 'Timestamp' },
        { value: 'decimal', title: 'Floating Number' },
        { value: 'string', title: 'Character Varying' },
        { value: 'text', title: 'Text' }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.cacheStore.getAccess
    },
    tables () {
      if (!this.database) {
        return []
      }
      return this.database.tables
    },
    isContrastTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast')
    },
    isDarkTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().startsWith('dark')
    },
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
    }
  },
  methods: {
    sizeToHumanLabel,
    closed (data) {
      console.debug('closed dialog', data)
      this.dialogSemantic = false
    },
    created (created) {
      return formatTimestampUTCLabel(created)
    },
    clazz (view) {
      return this.hasPublishedIdentifier(view) ? 'primary-text' : null
    },
    subtitle (table) {
      if (!table.description) {
        return sizeToHumanLabel(table.data_length)
      }
      return table.description
    },
    hasPublishedIdentifier (subset) {
      if (!subset.identifiers) {
        return null
      }
      return subset.identifiers.filter(i => i.status === 'published').length > 0
    }
  }
}
</script>
<style lang="scss" scoped>
.v-list {
  padding-top: 0;
  padding-bottom: 0;
}
</style>
