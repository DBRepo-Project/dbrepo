<template>
  <div v-if="user">
    <DBToolbar ref="toolbar" />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="isOwner" flat tile>
          <v-card-title>Access</v-card-title>
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
          <v-card-title>Visibility</v-card-title>
          <v-card-text>
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
              color="warning"
              class="black--text"
              @click="updateDatabaseVisibility">
              Modify Visibility
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="canModifyOwnership" flat tile>
          <v-card-title>Ownership</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col sm="6">
                <v-select
                  id="owner"
                  v-model="modifyOwner.username"
                  :items="users"
                  item-text="username"
                  item-value="username"
                  label="Owner"
                  name="owner" />
              </v-col>
            </v-row>
            <v-btn
              small
              color="warning"
              class="black--text"
              @click="updateDatabaseOwner">
              Modify Ownership
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
import { modifyVisibility, modifyOwnership } from '@/api/database'

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
      users: [],
      loading: false,
      loadingUsers: false,
      editAccessDialog: false,
      editVisibilityDialog: false,
      modifyVisibility: {
        is_public: null
      },
      modifyOwner: {
        username: null
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
    },
    canModifyOwnership () {
      if (!this.isOwner) {
        return false
      }
      return this.user.roles.includes('modify-database-owner')
    }
  },
  watch: {
    database (val) {
      if (!val) {
        return
      }
      this.modifyVisibility.is_public = this.database.is_public
      this.modifyOwner.username = this.database.owner.username
    }
  },
  mounted () {
    this.loadUsers()
    if (!this.database) {
      return
    }
    this.modifyVisibility.is_public = this.database.is_public
    this.modifyOwner.username = this.database.owner.username
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
    async updateDatabaseOwner () {
      try {
        this.loading = true
        await modifyOwnership(this.token, this.$route.params.container_id, this.$route.params.database_id, this.modifyOwner.username)
        this.$toast.success('Successfully updated the database owner')
      } catch (error) {
        console.error('Failed to update database owner', error)
        this.$toast.error('Failed to update database owner')
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
    async loadUsers () {
      this.loadingUsers = true
      try {
        const res = await this.$axios.get('/api/user', this.config)
        this.users = res.data
        console.debug('users', this.users)
      } catch (error) {
        console.error('Failed to load users', error)
        const { message } = error.response.data
        this.$toast.error(`Failed to load users: ${message}`)
      }
      this.loadingUsers = false
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
