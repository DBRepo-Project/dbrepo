<template>
  <div v-if="view">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/database/${$route.params.database_id}/view`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        <span v-if="view.name">{{ view.name }}</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canDeleteView" :loading="loadingDelete" color="error" class="mb-1" @click="deleteView">
          <v-icon left>mdi-delete</v-icon> Delete
        </v-btn>
        <v-btn v-if="canCreatePid" class="mb-1 ml-2" color="primary" :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/persist`">
          <v-icon left>mdi-content-save-outline</v-icon> Get PID
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-tabs v-model="tab" color="primary">
      <v-tab :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/info`">
        Info
      </v-tab>
      <v-tab :to="`/database/${$route.params.database_id}/view/${$route.params.view_id}/data`">
        Data
      </v-tab>
    </v-tabs>
  </div>
</template>

<script>
import UserUtils from '@/api/user.utils'
import DatabaseService from '@/api/database.service'

export default {
  components: {
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingDelete: false
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    view () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    canDeleteView () {
      if (!this.roles || !this.user || !this.view || !this.view.creator) {
        return false
      }
      return this.roles.includes('delete-database-view') && this.view.creator.id === this.user.id
    },
    canCreatePid () {
      if (!this.roles || !this.user || !this.view) {
        return false
      }
      return this.roles.includes('create-identifier') && UserUtils.hasReadAccess(this.access)
    },
    access () {
      return this.$store.state.access
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    }
  },
  methods: {
    deleteView () {
      this.loadingDelete = true
      DatabaseService.deleteView(this.$route.params.database_id, this.$route.params.view_id)
        .then(async () => {
          this.$toast.success('Successfully deleted view!')
          await this.$store.dispatch('reloadDatabase')
          await this.$router.push(`/database/${this.$route.params.database_id}/view`)
        })
        .finally(() => {
          this.loadingDelete = false
        })
    }
  }
}
</script>
