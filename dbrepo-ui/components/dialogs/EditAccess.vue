<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="updateAccess">
      <v-card
        :title="$t('pages.database.subpages.access.title')">
        <v-card-text>
          <p v-if="isModification">{{ username }}</p>
          <v-row>
            <v-col>
              <v-autocomplete
                v-if="!isModification"
                v-model="localUsername"
                :items="eligibleUsers"
                :disabled="loadingUsers"
                :loading="loadingUsers"
                :rules="[v => !!v || $t('validation.required')]"
                required
                :variant="inputVariant"
                hide-selected
                hide-details
                item-value="username"
                item-title="qualified_name"
                single-line
                persistent-hint
                :label="$t('pages.database.subpages.access.qualifiedname.label')"
                :hint="$t('pages.database.subpages.access.qualifiedname.hint')" />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-select
                v-model="modify.type"
                :items="accessTypes"
                :variant="inputVariant"
                :rules="[v => !!v || $t('validation.required')]"
                required
                persistent-hint
                :label="$t('pages.database.subpages.access.type.label')"
                :hint="$t('pages.database.subpages.access.type.hint')" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            :variant="buttonVariant"
            :text="$t('navigation.cancel')"
            @click="cancel" />
          <v-btn
            id="database"
            variant="flat"
            :disabled="!valid || loading || !canSubmit"
            :color="buttonColor"
            type="submit"
            :text="$t('navigation.modify')"
            :loading="loading"
            />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'
import { isSecondaryReplica } from '@/utils'

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
      localUsername: null,
      types: [
        { title: this.$t('pages.database.subpages.access.read'), value: 'read' },
        { title: this.$t('pages.database.subpages.access.write-own'), value: 'write_own' },
        { title: this.$t('pages.database.subpages.access.write-all'), value: 'write_all' },
        { title: this.$t('pages.database.subpages.access.revoke'), value: 'revoke' }
      ],
      modify: {
        type: null
      },
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    accessTypes () {
      const roles = this.cacheStore.getRoles || []
      return this.types.filter(t =>
        (!this.secondaryReplica || t.value === 'read' || t.value === 'revoke') &&
        (t.value === 'revoke' ? this.isModification && roles.includes('delete-database-access')
          : roles.includes(this.isModification ? 'update-database-access' : 'create-database-access')))
    },
    secondaryReplica () {
      return isSecondaryReplica(this.database, this.$config.public.api.client)
    },
    canSubmit () {
      return this.localUsername && this.modify.type !== this.accessType &&
        this.accessTypes.some(t => t.value === this.modify.type)
    },
    eligibleUsers () {
      return this.users.filter(u => !this.database.accesses.map(a => a.user.username).includes(u.username))
    },
    buttonColor () {
      if (!this.valid || this.loading || !this.canSubmit) {
        return null
      }
      if (this.modify.type && this.modify.type === 'revoke') {
        return 'error'
      }
      return 'warning'
    },
    isModification () {
      return this.username !== null
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
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
    cancel () {
      this.$emit('close-dialog', { success: false })
    },
    async updateAccess () {
      if (this.loading || !this.canSubmit || !(await this.$refs.form.validate()).valid) return
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
      const accessService = useAccessService()
      return accessService.remove(this.$route.params.database_id, this.localUsername)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.access.revoked', { access: this.modify.type }))
          this.$emit('close-dialog', { success: true, username: this.localUsername })
        })
        .catch(error => this.accessFailed(error, 'remove'))
        .finally(() => {
          this.loading = false
        })
    },
    modifyAccess () {
      const accessService = useAccessService()
      return accessService.update(this.$route.params.database_id, this.localUsername, this.modify)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.access.modified', { access: this.modify.type }))
          this.$emit('close-dialog', { success: true, username: this.localUsername })
        })
        .catch(error => this.accessFailed(error, 'update'))
        .finally(() => {
          this.loading = false
        })
    },
    giveAccess () {
      const accessService = useAccessService()
      return accessService.create(this.$route.params.database_id, this.localUsername, this.modify)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.access.created', { access: this.modify.type }))
          this.$emit('close-dialog', { success: true, username: this.localUsername })
        })
        .catch(error => this.accessFailed(error, 'create'))
        .finally(() => {
          this.loading = false
        })
    },
    accessFailed ({code, message}, operation) {
      if (code === 'error.access.dashboard_sync') {
        this.$emit('close-dialog', {success: true, dashboardSync: {
          username: this.localUsername, operation, type: this.modify.type
        }})
        return
      }
      const toast = useToastInstance()
      toast.error(typeof code === 'string' ? this.$t(code) : message)
    },
    loadUsers () {
      this.loadingUsers = true
      const userService = useUserService()
      userService.findAll()
        .then((users) => {
          this.users = users.filter(u => u.username !== this.database.owner.username)
        })
        .catch(({code, message}) => {
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingUsers = false
        })
    },
    init () {
      this.localUsername = this.username
      if (!this.username) {
        this.loadUsers()
      } else {
        this.localUsername = this.username
      }
      if (!this.accessType) {
        this.modify.type = null
      } else {
        this.modify.type = this.secondaryReplica ? 'read' : this.accessType
      }
    }
  }
}
</script>
