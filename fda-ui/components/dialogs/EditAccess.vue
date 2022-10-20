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
          <v-text-field
            v-if="isModification"
            v-model="access.user.username"
            label="Username"
            :rules="[v => !!v || $t('Required')]"
            required
            :disabled="isModification" />
          <v-text-field
            v-if="!isModification"
            v-model="modify.username"
            label="Username"
            :rules="[v => !!v || $t('Required')]"
            required
            :disabled="isModification" />
          <v-select
            v-if="isModification"
            v-model="access.type"
            :items="types"
            :rules="[v => !!v || $t('Required')]"
            required
            label="Access type" />
          <v-select
            v-if="!isModification"
            v-model="modify.type"
            :items="types"
            :rules="[v => !!v || $t('Required')]"
            required
            label="Access type" />
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
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog', { success: false })
    },
    async updateAccess () {
      if (this.isModification) {
        if (this.access.type === 'revoke') {
          await this.revokeAccess(this.access.user.username)
        } else {
          await this.modifyAccess(this.access.user.username)
        }
      } else {
        await this.giveAccess(this.access.user.username)
      }
    },
    async revokeAccess (username) {
      this.loading = true
      try {
        const res = await this.$axios.delete(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${username}`, this.config)
        console.debug('revoke access', res.data)
        this.$toast.success(`Successfully revoked access of ${username}`)
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        this.$toast.error('Could not revoke access to database')
      }
      this.loading = false
    },
    async modifyAccess (username) {
      if (this.isModification) {
        this.modify.username = this.access.user.username
        this.modify.type = this.access.type
      }
      this.loading = true
      try {
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${username}`, this.modify, this.config)
        console.debug('give access', res.data)
        this.$toast.success('Successfully modified access')
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        this.$toast.error('Could not modify access to database')
      }
      this.loading = false
    },
    async giveAccess (username) {
      this.loading = true
      try {
        const res = await this.$axios.post(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/access/${username}`, this.modify, this.config)
        console.debug('give access', res.data)
        this.$toast.success(`Successfully gave ${username} access`)
        this.$emit('close-dialog', { success: true })
      } catch (err) {
        this.$toast.error('Could not give access to database')
      }
      this.loading = false
    }
  }
}
</script>
