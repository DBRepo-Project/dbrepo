<template>
  <div v-if="isDeveloper">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
          <v-card-title>Personal Access Tokens</v-card-title>
          <v-card-subtitle>Authentication tokens to access the HTTP API</v-card-subtitle>
          <v-card-text>
            <v-list-item v-for="(item, i) in tokens" :key="i" three-line>
              <v-list-item-content>
                <v-list-item-title :class="tokenClass(item)">sha256:{{ item.token_hash }}</v-list-item-title>
                <v-list-item-subtitle v-if="!item.token" :class="tokenClass(item)">
                  Last used: <span v-if="item.last_used">{{ format(item.last_used) }}</span><span v-if="!item.last_used">Never</span> &mdash; valid until: {{ format(item.expires) }}
                </v-list-item-subtitle>
                <v-list-item-subtitle v-if="item.token">
                  <v-text-field
                    v-model="item.token"
                    :append-outer-icon="item.copied ? 'mdi-check' : 'mdi-content-copy'"
                    readonly
                    hint="Copy this token, it will not be visible again!"
                    persistent-hint
                    type="text"
                    @click:append-outer="copy(item)" />
                </v-list-item-subtitle>
                <v-list-item-subtitle v-if="!item.token">
                  <a @click="revokeToken(item.id)">Revoke Token</a>
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-btn
              v-if="isResearcher || isDeveloper"
              :disabled="tokens.length >= tokenMax"
              class="mt-4"
              color="secondary"
              small
              @click="mintToken">
              Create Token
            </v-btn>
          </v-card-text>
          <v-divider />
          <v-card-title>User Roles</v-card-title>
          <v-card-subtitle>Modify user roles</v-card-subtitle>
          <v-data-table
            :headers="headers"
            :items="users"
            :loading="loadingUsers"
            :items-per-page="10">
            <template v-slot:item.username="{ item }">
              {{ item.username }}
            </template>
            <template v-slot:item.roles="{ item }">
              <div v-for="(role, idx) in item.roles" :key="idx">
                {{ formatRole(role) }}
              </div>
            </template>
            <template v-slot:item.action="{ item }">
              <v-btn
                v-if="item.username !== user.username"
                :disabled="isDeveloper1(item)"
                x-small
                @click="modifyRoles(item)">
                Modify
              </v-btn>
              <span v-if="item.username === user.username">(you)</span>
            </template>
          </v-data-table>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-dialog
      v-model="editRoleDialog"
      persistent
      max-width="640">
      <EditRoles :user="selectedUser" @close-dialog="closeDialog" />
    </v-dialog>
  </div>
</template>

<script>
import { formatTimestamp, isResearcher, isDeveloper } from '@/utils'
import EditRoles from '@/components/dialogs/EditRoles.vue'

export default {
  components: {
    EditRoles
  },
  data () {
    return {
      tab: 0,
      error: false,
      tokens: [],
      loading: false,
      loadingUsers: false,
      users: [],
      editRoleDialog: false,
      selectedUser: {},
      roles: [
        { text: 'Researcher', value: 'researcher', code: 'ROLE_RESEARCHER' },
        { text: 'Data Steward', value: 'data_steward', code: 'ROLE_DATA_STEWARD' },
        { text: 'Developer', value: 'developer', code: 'ROLE_DEVELOPER' }
      ]
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    headers () {
      return [
        { text: 'Username', value: 'username', sortable: false },
        { text: 'Role', value: 'roles', sortable: false },
        { text: 'Action', value: 'action', sortable: false }
      ]
    },
    isDeveloper () {
      return isDeveloper(this.user)
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
    tokenMax () {
      return this.$config.tokenMax
    }
  },
  mounted () {
    this.loadTokens()
    this.loadUsers()
  },
  methods: {
    submit () {
    },
    copy (item) {
      item.copied = true
      navigator.clipboard.writeText(item.token)
    },
    format (timestamp) {
      return formatTimestamp(timestamp)
    },
    tokenClass (token) {
      return token.last_used ? '' : 'token-not_used'
    },
    isDeveloper1 (user) {
      return isDeveloper(user)
    },
    closeDialog (event) {
      if (event.success) {
        this.loadUsers()
      }
      this.editRoleDialog = false
    },
    modifyRoles (item) {
      this.selectedUser = item
      this.editRoleDialog = true
    },
    async loadTokens () {
      this.loading = true
      try {
        const res = await this.$axios.get('/api/user/token', this.config)
        this.tokens = res.data.filter(t => !t.deleted)
        console.debug('tokens', this.tokens)
      } catch (err) {
        this.$toast.error('Could not load tokens')
      }
      this.loading = false
    },
    async mintToken () {
      this.loading = true
      try {
        const res = await this.$axios.post('/api/user/token', {}, this.config)
        const token = res.data
        token.copied = false
        console.debug('token', token)
        this.tokens.push(token)
      } catch (err) {
        if (err.response.status === 417) {
          this.$toast.error('Already exceeded the maximum allowed number of tokens!')
        } else {
          this.$toast.error('Could not create token')
        }
      }
      this.loading = false
    },
    formatRole (role) {
      if (role === null) {
        return null
      }
      const arr = this.roles.filter(r => r.code === role)
      return arr.length > 0 ? arr[0].text : null
    },
    async loadUsers () {
      this.loadingUsers = true
      try {
        const res = await this.$axios.get('/api/user', this.config)
        this.users = res.data
        console.debug('users', this.users)
      } catch (error) {
        const { message } = error.response
        this.$toast.error('Failed to load users: ' + message)
        console.error('Failed to load users', error)
      }
      this.loadingUsers = false
    },
    async revokeToken (id) {
      this.loading = true
      try {
        await this.$axios.delete(`/api/user/token/${id}`, this.config)
        await this.loadTokens()
      } catch (err) {
        this.$toast.error('Could not delete token')
      }
      this.loading = false
    }
  }
}
</script>
<style>
.token-not_used {
  opacity: 0.4;
}
</style>
