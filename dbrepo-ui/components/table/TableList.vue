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
      v-for="(item, i) in tables"
      :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list>
        <v-list-item
          lines="two"
          :title="item.name"
          :subtitle="item.description ? item.description : '(no description)'"
          :to="`/database/${$route.params.database_id}/table/${item.id}/info`">
          <template v-slot:append>
            <v-tooltip
              v-if="item.identifiers && item.identifiers.length > 0"
              :text="$t('pages.identifier.pid.title')"
              left>
              <template v-slot:activator="{ props }">
                <v-icon
                  color="primary"
                  v-bind="props">mdi-identifier</v-icon>
              </template>
            </v-tooltip>
          </template>
        </v-list-item>
      </v-list>
    </v-card>
  </div>
</template>

<script>
import { formatTimestampUTCLabel } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
        { value: 'column_type', title: 'Type' },
        { value: 'column_concept', title: 'Concept' },
        { value: 'column_unit', title: 'Unit' },
        { value: 'is_primary_key', title: 'Primary Key' },
        { value: 'unique', title: 'Unique' },
        { value: 'is_null_allowed', title: 'Nullable' },
        { value: 'auto_generated', title: 'Sequence' }
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
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.userStore.getAccess
    },
    tables () {
      if (!this.database) {
        return []
      }
      return this.database.tables
    }
  },
  methods: {
    pick (item, mode) {
      this.column = item
      this.mode = mode
      this.dialogSemantic = true
    },
    closed (data) {
      console.debug('closed dialog', data)
      this.dialogSemantic = false
    },
    created (created) {
      return formatTimestampUTCLabel(created)
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
