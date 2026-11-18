<template>
  <div>
    <v-alert v-if="!database" type="info" variant="tonal">{{ $t('dataState.loading') }}</v-alert>
    <v-alert v-else-if="!database.is_schema_public" type="info" variant="tonal">
      {{ $t('dataState.hiddenSchema') }}
    </v-alert>
    <v-card
      v-if="database && views.length === 0"
      variant="flat"
      rounded="0"
      :text="$t('dataState.noViews')" />
    <div v-for="(view,i) in views" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list>
        <v-list-item
          lines="two"
          :title="view.name"
          :subtitle="view.query"
          :class="clazz(view)"
          :to="`/database/${$route.params.database_id}/view/${view.id}/info`">
          <template v-slot:append>
            <ResourceStatus
              :resource="view" />
          </template>
        </v-list-item>
      </v-list>
    </div>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      error: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      const database = this.cacheStore.getDatabase
      return database?.id === this.$route.params.database_id ? database : null
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.database.views
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
    clazz (view) {
      return this.hasPublishedIdentifier(view) ? 'primary-text' : null
    },
    hasPublishedIdentifier (view) {
      if (!view.identifiers) {
        return null
      }
      return view.identifiers.filter(i => i.status === 'published').length > 0
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
