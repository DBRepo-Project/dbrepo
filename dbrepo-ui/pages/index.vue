<template>
  <div>
    <v-toolbar
      :title="$t('toolbars.database.recent')"
      rounded="0"
      flat>
      <v-spacer />
      <v-btn
        v-if="canCreateDatabase"
        class="mr-2"
        prepend-icon="mdi-plus"
        variant="flat"
        :text="$t('toolbars.database.create.text')"
        color="secondary"
        @click.stop="dialog = true" />
    </v-toolbar>
    <v-alert v-if="failed" type="error" variant="tonal" role="alert">
      {{ $t('pages.database.list-error') }}
      <v-btn variant="text" @click="fetchDatabases">{{ $t('navigation.retry') }}</v-btn>
    </v-alert>
    <p v-else-if="!loading && !databases.length" class="pa-4" role="status">
      {{ $t('pages.database.list-empty') }}
    </p>
    <DatabaseList
      v-else
      v-cloak
      :loading="loading"
      :databases="databases" />
    <div class="text-center">
      <v-btn
        v-if="databases && databases.length > 0"
        class="mt-2"
        variant="flat"
        to="/search"
        color="plain">
        {{ $t('navigation.more')}}
      </v-btn>
    </div>
    <v-dialog
      v-model="dialog"
      persistent
      max-width="640">
      <DatabaseCreate @close="closed" />
    </v-dialog>
  </div>
</template>

<script>
import DatabaseList from '@/components/database/DatabaseList.vue'
import DatabaseCreate from '@/components/database/DatabaseCreate.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    DatabaseCreate,
    DatabaseList
  },
  data () {
    return {
      loading: true,
      failed: false,
      dialog: null,
      databases: [],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    roles () {
      return this.cacheStore.getRoles
    },
    canCreateDatabase () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database')
    }
  },
  mounted () {
    this.fetchDatabases()
  },
  methods: {
    closed () {
      this.dialog = false
    },
    async fetchDatabases () {
      this.loading = true
      this.failed = false
      try {
        this.databases = await useDatabaseService().findAll()
      } catch {
        this.failed = true
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
