<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-card-title v-text="title" />
        <v-card-subtitle v-if="subtitle" v-text="subtitle" />
        <v-card-text>
          <v-row>
            <v-col>
              <v-autocomplete
                v-if="!isModification"
                v-model="modify.userId"
                :items="eligibleUsers"
                :disabled="loadingUsers"
                :loading="loadingUsers"
                :rules="[v => !!v || $t('Required')]"
                required
                hide-no-data
                hide-selected
                hide-details
                item-text="qualified_name"
                item-value="id"
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
            :disabled="!valid || loading || accessType === modify.type"
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
    userId: {
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
        userId: null,
        type: null
      }
    }
  },
  computed: {
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
      return this.userId !== null
    },
    buttonText () {
      return (this.isModification ? 'Modify' : 'Create')
    }
  },
  watch: {
    userId () {
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
      this.loading = true
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
      DatabaseService.revokeAccess(this.$route.params.database_id, this.modify.userId)
        .then(() => {
          this.$toast.success('Successfully revoked access')
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    modifyAccess () {
      DatabaseService.modifyAccess(this.$route.params.database_id, this.modify.userId, this.modify.type)
        .then(() => {
          this.$toast.success('Successfully modified access')
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    giveAccess () {
      DatabaseService.giveAccess(this.$route.params.database_id, this.modify.userId, this.modify.type)
        .then(() => {
          this.$toast.success('Successfully provisioned access')
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
          this.loadingUsers = false
        })
    },
    init () {
      if (!this.userId) {
        this.modify.userId = null
        this.loadUsers()
      } else {
        this.modify.userId = this.userId
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
