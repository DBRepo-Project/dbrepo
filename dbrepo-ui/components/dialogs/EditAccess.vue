<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title v-text="title" />
        <v-card-text>
          <v-alert
            v-if="modify.type && modify.type !== 'revoke'"
            border="left"
            color="warning">
            <strong>Dangerous operation:</strong> you are giving this user access to <strong>{{ explanation }}</strong> in your database
          </v-alert>
          <v-alert
            v-if="modify.type && modify.type === 'revoke'"
            border="left"
            color="error">
            <strong>Dangerous operation:</strong> you are <strong>revoking</strong> all access for this user to your database
          </v-alert>
          <v-row>
            <v-col>
              <v-autocomplete
                v-model="modify.username"
                :items="eligableUsers"
                :loading="loadingUsers"
                :rules="[v => !!v || $t('Required')]"
                required
                hide-no-data
                hide-selected
                hide-details
                item-text="username"
                item-value="username"
                :disabled="isModification"
                single-line
                label="Username" />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-select
                v-model="modify.type"
                :items="accessTypes"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Access type" />
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
            class="mb-2 ml-3 mr-2 black--text"
            :disabled="!valid || loading"
            :color="buttonColor"
            type="submit"
            :loading="loading"
            @click="updateAccess">
            {{ buttonText }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    username: {
      type: String,
      default () {
        return null
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      loadingUsers: false,
      users: [],
      error: false,
      types: [
        { text: 'Read', value: 'read' },
        { text: 'Write access (restricted)', value: 'write_own' },
        { text: 'Full access', value: 'write_all' },
        { text: 'Revoke all access', value: 'revoke' }
      ],
      modify: {
        username: null,
        type: null
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
    database () {
      return this.$store.state.database
    },
    title () {
      return (!this.isModification ? 'Give' : 'Modify') + ' database access' + (!this.isModification ? '' : ` of ${this.username}`)
    },
    accessTypes () {
      if (!this.isModification) {
        /* give access cannot revoke access */
        return this.types.filter(t => t.value !== 'revoke')
      }
      return this.types
    },
    eligableUsers () {
      return this.users.filter(u => !this.database.accesses.map(a => a.user.id).includes(u.id))
    },
    buttonColor () {
      if (this.modify.type && this.modify.type === 'revoke') {
        return 'error'
      }
      return 'warning'
    },
    isModification () {
      return this.username !== null
    },
    explanation () {
      switch (this.modify.type) {
        case 'read':
          return 'read all contents and create subsets'
        case 'write_own':
          return 'read all contents, create subsets and write their own tables'
        case 'write_all':
          return 'read all contents, create subsets and write all tables'
        default:
          return ''
      }
    },
    buttonText () {
      return (this.isModification ? 'Modify' : 'Give') + ' Access'
    }
  },
  watch: {
    username (val) {
      if (!val || this.users.length === 0) {
        this.modify.username = null
      }
      this.selectUser()
    }
  },
  mounted () {
    this.loadUsers()
      .then(() => this.selectUser())
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog', { success: false })
    },
    async updateAccess () {
      if (this.isModification) {
        if (this.modify.type === 'revoke') {
          await this.revokeAccess()
        } else {
          await this.modifyAccess()
        }
      } else {
        await this.giveAccess()
      }
    },
    async revokeAccess () {
      this.loading = true
      try {
        const res = await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${this.username}`, this.config)
        console.debug('revoke access', res.data)
        this.$toast.success(`Successfully revoked access of ${this.username}`)
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        console.log('revoke access', err)
        this.$toast.error('Could not revoke access to database')
      }
      this.loading = false
    },
    async modifyAccess () {
      this.loading = true
      try {
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${this.username}`, {
          type: this.modify.type
        }, this.config)
        console.debug('give access', res.data)
        this.$toast.success('Successfully modified access')
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        console.log('modify access', err)
        this.$toast.error('Could not modify access to database')
      }
      this.loading = false
    },
    async giveAccess () {
      const username = this.modify.username
      this.loading = true
      try {
        const res = await this.$axios.post(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access`, this.modify, this.config)
        console.debug('give access', res.data)
        this.$toast.success(`Successfully gave ${username} access`)
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        if (err.response.status === 405) {
          this.$toast.error(`User ${username} already has access`)
          this.loading = false
          return
        } else if (err.response.status === 404) {
          this.$toast.error(`User ${username} does not exist`)
          this.loading = false
          return
        }
        console.log('give access', err)
        this.$toast.error('Could not give access to database')
      }
      this.loading = false
    },
    async loadUsers () {
      this.loadingUsers = true
      try {
        const res = await this.$axios.get('/api/user', this.config)
        this.users = res.data.filter(u => u.username !== this.database.creator.username)
        console.debug('users', this.users)
      } catch (error) {
        console.error('Failed to load users', error)
        const { message } = error.response.data
        this.$toast.error(`Failed to load users: ${message}`)
      }
      this.loadingUsers = false
    },
    selectUser () {
      const optional = this.users.filter(u => u.username === this.username)
      if (optional.length > 0) {
        this.modify.username = optional[0]
      }
    }
  }
}
</script>
