<template>
  <div v-if="user">
    <DBToolbar ref="toolbar" />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="isOwner" flat tile>
          <v-card-title>Modify database access</v-card-title>
          <v-card-subtitle>This is a dangerous operation</v-card-subtitle>
          <v-data-table
            :headers="headers"
            :items="database.accesses"
            :items-per-page="10">
            <template v-slot:item.user="{ item }">
              {{ item.user.username }}
            </template>
            <template v-slot:item.action="{ item }">
              <v-btn
                x-small
                @click="modifyAccess(item)">
                Modify
              </v-btn>
            </template>
          </v-data-table>
          <v-card-text>
            <v-btn
              small
              color="warning"
              class="black--text"
              @click="giveAccess">
              Give Access
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="canModifyVisibility" flat tile>
          <v-card-title>Modify database visibility</v-card-title>
          <v-card-subtitle>This is a dangerous operation</v-card-subtitle>
          <v-card-text>
            <v-alert
              v-if="database.is_public !== modifyVisibility.is_public"
              border="left"
              color="warning">
              <strong>Dangerous operation:</strong> you are about to change the visibility of the database. This affects all (sensitive) data held in the database.
            </v-alert>
            <v-row dense>
              <v-col sm="6">
                <v-select
                  id="visibility"
                  v-model="modifyVisibility.is_public"
                  :items="visibility"
                  label="Visibility"
                  name="visibility" />
              </v-col>
            </v-row>
            <v-btn
              small
              :disabled="database.is_public === modifyVisibility.is_public"
              color="warning"
              class="black--text"
              @click="updateDatabaseVisibility">
              Modify Visibility
            </v-btn>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="editAccessDialog"
      max-width="640">
      <EditAccess :username="username" @close-dialog="closeDialog" />
    </v-dialog>
  </div>
</template>

<script>
import DBToolbar from '@/components/DBToolbar'
import EditAccess from '@/components/dialogs/EditAccess'
import { modifyVisibility } from '@/api/database'

export default {
  components: {
    DBToolbar,
    EditAccess
  },
  data () {
    return {
      dialogDelete: false,
      confirm: null,
      username: null,
      loading: false,
      editAccessDialog: false,
      editVisibilityDialog: false,
      modifyVisibility: {
        is_public: null
      },
      visibility: [
        { text: 'Public', value: true },
        { text: 'Private', value: false }
      ],
      headers: [
        { text: 'Username', value: 'user', sortable: false },
        { text: 'Access', value: 'type', sortable: false },
        { text: 'Action', value: 'action', sortable: false }
      ],
      accesses: [],
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tab () {
      return 0
    },
    database () {
      return this.$store.state.database
    },
    access () {
      return this.$store.state.access
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    user () {
      return this.$store.state.user
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      if (this.database.owner.username === null || this.user.username === null) {
        return false
      }
      return this.database.owner.username === this.user.username
    },
    canModifyVisibility () {
      if (!this.isOwner) {
        return false
      }
      return this.user.roles.includes('modify-database-visibility')
    }
  },
  watch: {
    database (val) {
      if (!val) {
        return
      }
      this.modifyVisibility.is_public = this.database.is_public
    }
  },
  mounted () {
    if (!this.database) {
      return
    }
    this.modifyVisibility.is_public = this.database.is_public
  },
  methods: {
    closeDialog (event) {
      if (event.success) {
        this.loadDatabase()
      }
      this.loadDatabase()
      this.editAccessDialog = false
    },
    async updateDatabaseVisibility () {
      try {
        this.loading = true
        await modifyVisibility(this.token, this.$route.params.container_id, this.$route.params.database_id, this.modifyVisibility.is_public)
        this.$toast.success('Successfully updated the database visibility')
        location.reload()
      } catch (error) {
        console.error('Failed to update database visibility', error)
        this.$toast.error('Failed to update database visibility')
      }
      this.loading = false
    },
    giveAccess () {
      this.username = null
      this.editAccessDialog = true
    },
    modifyAccess (item) {
      this.username = item.user.username
      this.editAccessDialog = true
    },
    async loadDatabase () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.$store.commit('SET_DATABASE', res.data)
        console.debug('database', this.database)
      } catch (err) {
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
    }
  }
}
</script>
