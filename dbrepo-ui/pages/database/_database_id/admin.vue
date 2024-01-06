<template>
  <div v-if="db">
    <DatabaseToolbar />
    <v-tabs-items v-model="tab">
      <v-card flat>
        <v-card-title>
          Database Administration
        </v-card-title>
        <v-card-text>
          <v-btn outlined color="error" @click="dialogDelete = true">Delete</v-btn>
        </v-card-text>
      </v-card>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog v-model="dialogDelete" max-width="500">
      <v-card>
        <v-card-title class="headline">
          Delete
        </v-card-title>
        <v-card-text class="pb-1">
          Are you sure to drop this database? Confirm the deletion by typing the database internal name
          <strong>{{ db.internalName }}</strong> in the text box below.
          <v-text-field v-model="confirm" label="Database Name" />
        </v-card-text>
        <v-card-actions class="pl-4 pb-4 pr-4">
          <v-btn @click="dialogDelete=false">
            Cancel
          </v-btn>
          <v-spacer />
          <v-btn :disabled="canDelete" color="error" @click="deleteDatabase()">
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import DatabaseToolbar from '@/components/DatabaseToolbar.vue'
import DatabaseService from '@/api/database.service'

export default {
  components: {
    DatabaseToolbar
  },
  data () {
    return {
      dialogDelete: false,
      confirm: null,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tab () {
      return 3
    },
    db () {
      return this.$store.state.database
    },
    canDelete () {
      if (this.confirm === null) {
        return true
      }
      return this.confirm !== this.db.internalName
    }
  },
  methods: {
    deleteDatabase () {
      DatabaseService.delete(this.$route.params.database_id)
        .then(async () => {
          this.$toast.success(`Database "${this.db.name}" deleted.`)
          await this.$router.push({ path: '/databases' })
          this.dialogDelete = false
        })
    }
  }
}
</script>
