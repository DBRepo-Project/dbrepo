<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="submit">
      <v-card
        :title="$t('pages.database.subpages.access.title')">
        <v-card-text>
          <v-row>
            <v-col>
              <v-autocomplete
                v-if="!isModification"
                v-model="modify.userId"
                :items="eligibleUsers"
                :disabled="loadingUsers"
                :loading="loadingUsers"
                :rules="[v => !!v || $t('validation.required')]"
                required
                :variant="inputVariant"
                hide-no-data
                hide-selected
                hide-details
                item-value="id"
                item-title="qualified_name"
                single-line
                persistent-hint
                :label="$t('pages.database.subpages.access.username.label')"
                :hint="$t('pages.database.subpages.access.username.hint')" />
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
            :disabled="!valid || loading || accessType === modify.type"
            :color="buttonColor"
            type="submit"
            :text="$t('pages.database.subpages.access.submit.text')"
            :loading="loading"
            @click="updateAccess" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'

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
      if (!this.valid || this.loading || this.accessType === this.modify.type) {
        return null
      }
      if (this.modify.type && this.modify.type === 'revoke') {
        return 'error'
      }
      return 'warning'
    },
    isModification () {
      return this.userId !== null
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
      const accessService = useAccessService()
      accessService.remove(this.$route.params.database_id, this.userId)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('notifications.access.revoked'))
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    modifyAccess () {
      const accessService = useAccessService()
      accessService.modify(this.$route.params.database_id, this.userId, this.modify)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('notifications.access.modified'))
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    giveAccess () {
      const accessService = useAccessService()
      accessService.create(this.$route.params.database_id, this.userId, this.modify)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('notifications.access.created'))
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadUsers () {
      this.loadingUsers = true
      const userService = useUserService()
      userService.findAll()
        .then((users) => {
          this.users = users.filter(u => u.username !== this.database.creator.username)
        })
        .finally(() => {
          this.loadingUsers = false
        })
    },
    init () {
      if (!this.userId) {
        this.loadUsers()
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
