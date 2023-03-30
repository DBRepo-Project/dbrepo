<template>
  <div v-if="table">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        {{ table.name }}
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canAddTuple" class="mr-2 mb-1" @click="addTuple">
          <v-icon left>mdi-plus</v-icon> Add
        </v-btn>
        <v-btn v-if="canEditTuple" color="warning" class="mr-2 mb-1 black--text" @click="editTuple">
          <v-icon left>mdi-pencil</v-icon> Edit
        </v-btn>
        <v-btn v-if="canDeleteTuple" color="error" class="mr-2 mb-1" :loading="loadingDelete" @click="deleteItems">
          <v-icon left>mdi-delete</v-icon> Delete<span v-if="selection.length > 1">&nbsp;{{ selection.length }}</span>
        </v-btn>
        <v-btn v-if="canExecuteQuery" class="mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/query/create?tid=${$route.params.table_id}`" color="secondary">
          <v-icon left>mdi-wrench</v-icon> Create Subset
        </v-btn>
        <v-btn v-if="canCreateView" class="ml-2 mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/view/create?tid=${$route.params.table_id}`" color="secondary">
          <v-icon left>mdi-view-carousel</v-icon> Create View
        </v-btn>
        <v-btn v-if="canImportCsv" class="ml-2 mb-1" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/import`">
          <v-icon left>mdi-cloud-upload</v-icon> Import csv
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-tabs v-model="tab" color="primary">
      <v-tab :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/info`">
        Info
      </v-tab>
      <v-tab v-if="canReadData" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/data`">
        Data
      </v-tab>
      <v-tab :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}/schema`">
        Schema
      </v-tab>
    </v-tabs>
    <v-dialog
      v-model="editTupleDialog"
      persistent
      max-width="640">
      <EditTuple :tuple="tuple" :edit="edit" @close="close" />
    </v-dialog>
  </div>
</template>

<script>
import EditTuple from '@/components/dialogs/EditTuple'
import { isResearcher } from '@/utils'

export default {
  components: {
    EditTuple
  },
  props: {
    selection: {
      type: Array,
      default: () => {
        return []
      }
    }
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingDelete: false,
      error: false,
      edit: false,
      editTupleDialog: false
    }
  },
  computed: {
    loadingColor () {
      return 'primary'
    },
    database () {
      return this.$store.state.database
    },
    table () {
      return this.$store.state.table
    },
    access () {
      return this.$store.state.access
    },
    user () {
      return this.$store.state.user
    },
    token () {
      return this.$store.state.token
    },
    canAddTuple () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('insert-table-data')
    },
    canEditTuple () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('insert-table-data')
    },
    canDeleteTuple () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('delete-table-data')
    },
    canExecuteQuery () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('execute-query')
    },
    canCreateView () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('create-database-view')
    },
    canReadData () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('view-table-data')
    },
    canImportCsv () {
      if (!this.user) {
        return false
      }
      return this.user.roles.includes('insert-table-data')
    },
    tuple () {
      return this.edit ? this.selection[0] : {}
    },
    isOwner () {
      if (!this.user || !this.database || !this.database.creator) {
        return false
      }
      return this.database.creator.username === this.user.username
    },
    isResearcher () {
      return isResearcher(this.user)
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    silentConfig () {
      return {
        headers: this.config.headers,
        progress: false
      }
    },
    databaseTooltip () {
      return this.database.is_public ? 'Public' : 'Private'
    }
  },
  watch: {
    selection (newVersion, oldVersion) {
      console.info('selected new', this.selection)
    }
  },
  methods: {
    addTuple () {
      const data = {}
      this.edit = false
      this.table.columns.forEach((c) => {
        data[c.internal_name] = null
      })
      this.selection = []
      this.editTupleDialog = true
    },
    editTuple () {
      this.edit = true
      this.editTupleDialog = true
    },
    pick () {
      if (this.$refs.timeTravel !== undefined) {
        /* when the component was loaded once, this method refreshes the content */
        this.$refs.timeTravel.loadHistory()
      }
      this.pickVersionDialog = true
    },
    async deleteItems () {
      if (this.selection.length < 1) {
        return
      }
      try {
        this.loadingDelete = true
        for (const select of this.selection) {
          /* remove in container */
          const constraints = {}
          this.table.columns
            .filter(c => c.is_primary_key)
            .forEach((c) => {
              constraints[c.internal_name] = select[c.internal_name]
            })
          await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data`, {
            headers: { Authorization: `Bearer ${this.token}` },
            data: { keys: constraints }
          })
        }
        console.info(`Deleted ${this.selection.length} rows(s)`)
        this.$toast.success(`Deleted ${this.selection.length} rows(s)`)
        this.$emit('modified', { success: true, action: 'delete' })
      } catch (error) {
        console.error('Failed to delete rows', error)
        const { data } = error.response
        const { message } = data
        this.$toast.error(`Failed to delete rows: ${message}`)
      }
      this.loadingDelete = false
    },
    close (event) {
      console.debug('closed edit/create tuple dialog', event)
      this.editTupleDialog = false
      if (event.success) {
        this.$emit('modified', { success: true, action: 'save' })
      } else {
        this.$emit('modified', { success: false, action: 'close' })
      }
    }
  }
}
</script>
