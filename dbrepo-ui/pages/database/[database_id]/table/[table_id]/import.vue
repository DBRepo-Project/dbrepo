<template>
  <div
    v-if="canInsertTableData">
    <v-toolbar flat>
      <v-btn
        class="mr-2"
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        :to="`/database/${$route.params.database_id}/table`" />
      <v-toolbar-title
        :text="title" />
    </v-toolbar>
    <v-card
      variant="flat"
      rounded="0">
      <v-card-text>
        <v-stepper
          vertical
          variant="flat">
          <TableImport
            :create="false"
            :table-id="$route.params.table_id" />
        </v-stepper>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import TableImport from '@/components/table/TableImport.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    TableImport
  },
  data () {
    return {
      loading: false,
      step: 1,
      ready: false,
      file: {
        filename: null,
        path: null
      },
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.tables'),
          to: `/database/${this.$route.params.database_id}/table`
        },
        {
          title: `${this.$route.params.table_id}`,
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/info`
        },
        {
          title: this.$t('navigation.import'),
          to: `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/import`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    table () {
      return this.cacheStore.getTable
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    access () {
      return this.cacheStore.getAccess
    },
    title () {
      if (!this.table) {
        return this.$t('pages.table.import.title')
      }
      return this.$t('pages.table.import.title') + ' ' + this.table.name
    },
    canInsertTableData () {
      if (!this.table || !this.access || !this.cacheUser || !this.roles || !this.roles.includes('insert-table-data')) {
        return false
      }
      const userService = useUserService()
      return userService.hasWriteAccess(this.table, this.access, this.cacheUser)
    }
  }
}
</script>
