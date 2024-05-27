<template>
  <div>
    <v-card
      v-if="!loading && views.length === 0"
      variant="flat"
      rounded="0"
      :text="$t('pages.database.subpages.views.empty')" />
    <div v-for="(view,i) in views" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list>
        <v-list-item
          lines="two"
          :title="view.name"
          :class="clazz(view)"
          :to="`/database/${$route.params.database_id}/view/${view.id}/info`">
          <template v-slot:append>
            <v-tooltip
              v-if="hasPublishedIdentifier(view)"
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
    </div>
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      error: false,
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.database.views
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
