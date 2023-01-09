<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title>
          User Role
        </v-card-title>
        <v-card-subtitle>
          Modify user role
        </v-card-subtitle>
        <v-card-text>
          <v-alert
            v-if="becomeDeveloper"
            border="left"
            color="warning">
            <strong>Dangerous operation:</strong> you are giving this user developer access. This cannot be undone.
          </v-alert>
          <v-row>
            <v-col>
              <v-autocomplete
                v-model="selectedUser"
                :items="users"
                :loading="loadingUsers"
                :rules="[v => !!v || $t('Required')]"
                required
                hide-no-data
                hide-selected
                hide-details
                return-object
                item-text="username"
                item-value="username"
                single-line
                label="Username" />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-select
                v-model="modify.roles"
                :items="roles"
                multiple
                item-value="code"
                item-text="text"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Role type" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            id="database"
            class="mb-2 ml-3 mr-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            :loading="loading"
            @click="updateRoles">
            Save
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    user: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      loadingUsers: false,
      selectedUser: null,
      users: [],
      error: false,
      roles: [
        { text: 'Researcher', value: 'researcher', code: 'ROLE_RESEARCHER' },
        { text: 'Data Steward', value: 'data_steward', code: 'ROLE_DATA_STEWARD' },
        { text: 'Developer', value: 'developer', code: 'ROLE_DEVELOPER' }
      ],
      modify: {
        roles: []
      }
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
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
    becomeDeveloper () {
      return this.modify.roles.filter(r => r === 'ROLE_DEVELOPER').length > 0
    }
  },
  watch: {
    user (newVal, oldVal) {
      this.modify.roles = newVal.roles
      this.selectedUser = newVal
    }
  },
  mounted () {
    this.loadUsers()
    this.modify.roles = this.user.roles
    this.selectedUser = this.user
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog', { success: false })
    },
    async updateRoles () {
      this.loading = true
      this.modify.roles = this.modify.roles.map(role => this.roles.filter(r => r.code === role)[0].value)
      try {
        const res = await this.$axios.put(`/api/user/${this.selectedUser.id}/roles`, this.modify, this.config)
        console.debug('roles', res.data)
        this.$toast.success('Updated user roles')
        this.$emit('close-dialog', { success: true })
      } catch (error) {
        const { message } = error.response
        this.$toast.error('Failed to update user roles: ' + message)
        console.error('Failed to update user roles', error)
      }
      this.loading = false
    },
    async loadUsers () {
      this.loading = true
      try {
        const res = await this.$axios.get('/api/user', this.config)
        this.users = res.data.map((user) => {
          user.roles_pretty = user.roles.join(',')
          return user
        })
        console.debug('users', this.users)
      } catch (error) {
        const { message } = error.response
        this.$toast.error('Failed to load users: ' + message)
        console.error('Failed to load users', error)
      }
      this.loading = false
    }
  }
}
</script>
