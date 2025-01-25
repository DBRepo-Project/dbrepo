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
    <DatabaseList
      v-cloak
      :loading="loading"
      :databases="databases" />
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

export default {
  components: {
    DatabaseCreate,
    DatabaseList
  },
  data () {
    return {
      loading: true,
      dialog: null,
      databases: []
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
    fetchDatabases () {
      this.loading = true
      const databaseService = useDatabaseService()
      databaseService.findAll()
        .then((databases) => {
          this.databases = databases
          this.loading = false
        })
    }
  }
}
</script>
