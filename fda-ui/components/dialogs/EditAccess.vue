<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit" autocomplete="off">
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
            color="warning">
            <strong>Dangerous operation:</strong> you are <strong>revoking</strong> all access for this user to your database
          </v-alert>
          <v-row>
            <v-col>
              <v-autocomplete
                v-model="modify.username"
                :items="users"
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
                :items="types"
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
            color="warning"
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
    database: {
      type: Object,
      default () {
        return {}
      }
    },
    access: {
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
      users: [],
      error: false,
      types: [
        { text: 'Read', value: 'read' },
        { text: 'Write access (restricted)', value: 'write_own' },
        { text: 'Write access', value: 'write_all' },
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
    title () {
      return (!this.isModification ? 'Give' : 'Modify') + ' database access' + (!this.isModification ? '' : ` of ${this.access.user.username}`)
    },
    isModification () {
      if (this.access == null) {
        return false
      }
      return this.access.user !== null
    },
    explanation () {
      switch (this.modify.type) {
        case 'read':
          return 'read all contents'
        case 'write_own':
          return 'write their own tables and read all contents'
        case 'write_all':
          return 'write all tables and read all contents'
        default:
          return ''
      }
    },
    buttonText () {
      return (this.isModification ? 'Modify' : 'Give') + ' Access'
    }
  },
  watch: {
    access (newVal, oldVal) {
      if (newVal == null) {
        this.modify.username = null
        this.modify.type = null
      } else {
        this.modify.username = newVal.user.username
        this.modify.type = newVal.type
      }
    }
  },
  mounted () {
    this.loadUsers()
    if (this.access === null) {
      return
    }
    this.modify.username = this.access.user.username
    this.modify.type = this.access.type
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
      const username = this.modify.username
      this.loading = true
      try {
        const res = await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${username}`, this.config)
        console.debug('revoke access', res.data)
        this.$toast.success(`Successfully revoked access of ${username}`)
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        console.log('revoke access', err)
        this.$toast.error('Could not revoke access to database')
      }
      this.loading = false
    },
    async modifyAccess () {
      const username = this.modify.username
      this.loading = true
      try {
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${username}`, {
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
      } catch (err) {
        console.log('users', err)
        this.$toast.error('Failed to load users')
      }
      this.loadingUsers = false
    }
  }
}
</script>
