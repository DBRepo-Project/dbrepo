<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title v-text="title" />
        <v-card-subtitle v-if="subtitle" v-text="subtitle" />
        <v-card-text>
          <div v-if="!isModification">
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
          </div>
          <v-row>
            <v-col>
              <v-autocomplete
                v-if="!isModification"
                v-model="modify.username"
                :items="eligibleUsers"
                :loading="loadingUsers"
                :rules="[v => !!v || $t('Required')]"
                required
                hide-no-data
                hide-selected
                hide-details
                item-text="username"
                item-value="username"
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
import DatabaseService from '@/api/database.service'
import UserService from '@/api/user.service'
export default {
  props: {
    username: {
      type: String,
      default () {
        return null
      }
    },
    accessType: {
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
      return (!this.isModification ? 'Give' : 'Modify') + ' database access'
    },
    subtitle () {
      return (this.isModification ? `User with username ${this.username}` : false)
    },
    accessTypes () {
      if (!this.isModification) {
        /* give access cannot revoke access */
        return this.types.filter(t => t.value !== 'revoke')
      }
      return this.types
    },
    eligibleUsers () {
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
    username () {
      this.init()
    },
    accessType () {
      this.init()
    }
  },
  mounted () {
    this.init()
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
    revokeAccess () {
      this.loading = true
      DatabaseService.revokeAccess(this.$route.params.container_id, this.$route.params.database_id, this.username)
        .then(() => {
          this.$toast.success(`Successfully revoked access of ${this.username}`)
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    modifyAccess () {
      this.loading = true
      DatabaseService.modifyAccess(this.$route.params.container_id, this.$route.params.database_id, this.username, this.modify.type)
        .then(() => {
          this.$toast.success('Successfully modified access')
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    giveAccess () {
      const username = this.modify.username
      this.loading = true
      DatabaseService.giveAccess(this.$route.params.container_id, this.$route.params.database_id, this.username, this.modify.type)
        .then(() => {
          this.$toast.success(`Successfully gave ${username} access`)
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadUsers () {
      this.loadingUsers = true
      UserService.findAll()
        .then((users) => {
          this.users = users.filter(u => u.username !== this.database.creator.username)
        })
        .finally(() => {
          this.loading = false
        })
    },
    init () {
      if (!this.username) {
        this.modify.username = null
        this.loadUsers()
      } else {
        this.modify.username = this.username
        /* eligible users are computed separately */
      }
      if (!this.accessType) {
        this.modify.type = null
      } else {
        this.modify.type = this.accessType
      }
    }
  }
}
</script>
