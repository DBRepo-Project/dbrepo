<template>
  <div>
    <UserToolbar />
    <v-window
      v-model="tab">
      <v-window-item>
        <v-card
          v-if="canHandleMessages"
          :title="$t('pages.settings.subpages.developer.maintenance.title')"
          rounded="0"
          variant="flat">
          <v-data-table
            :headers="headers"
            :items="messages"
            :loading="loadingMessages"
            :items-per-page="10">
            <template v-slot:item.action="{ item }">
              <v-btn
                size="x-small"
                variant="flat"
                :text="$t('pages.settings.subpages.developer.maintenance.modify.text')"
                @click="modifyMessage(item)" />
            </template>
          </v-data-table>
          <v-card-text>
            <v-btn
              size="small"
              variant="flat"
              :text="$t('pages.settings.subpages.developer.maintenance.add.text')"
              :disabled="!canCreateMessage"
              @click="createMessage" />
          </v-card-text>
        </v-card>
        <v-divider
          v-if="canHandleMessages" />
        <v-card
          :title="$t('pages.settings.subpages.developer.token.title')"
          :subtitle="$t('pages.settings.subpages.developer.token.subtitle')"
          variant="flat"
          rounded="0">
          <v-card-text>
            <v-row dense>
              <v-col xl="4">
                <v-text-field
                  v-model="accessTokenField"
                  disabled
                  :variant="inputVariant"
                  :label="$t('pages.settings.subpages.developer.token.access.label')" />
              </v-col>
              <v-col xl="2">
                <v-text-field
                  v-model="tokenExpiry"
                  disabled
                  :variant="inputVariant"
                  :label="expiryLabel(token)" />
              </v-col>
            </v-row>
            <v-row dense>
              <v-col xl="4">
                <v-text-field
                  v-model="refreshTokenField"
                  disabled
                  :variant="inputVariant"
                  :label="$t('pages.settings.subpages.developer.token.refresh.label')" />
              </v-col>
              <v-col xl="2">
                <v-text-field
                  v-model="refreshTokenExpiry"
                  disabled
                  :variant="inputVariant"
                  :label="expiryLabel(refreshToken)" />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="dialog"
      persistent
      max-width="640">
      <EditMaintenanceMessage
        :id="messageId"
        @close-dialog="closeDialog" />
    </v-dialog>
  </div>
</template>

<script>
import UserToolbar from '@/components/user/UserToolbar.vue'
import EditMaintenanceMessage from '@/components/dialogs/EditMaintenanceMessage.vue'
import { formatTimestampUTCLabel, isActiveMessage, timestampsToHumanDifference } from '@/utils'
import { useUserStore } from '@/stores/user.js'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    UserToolbar,
    EditMaintenanceMessage
  },
  data () {
    return {
      tab: 0,
      accessTokenField: null,
      refreshTokenField: null,
      headers: [
        { title: this.$t('pages.settings.subpages.developer.maintenance.active'), value: 'active' },
        { title: this.$t('pages.settings.subpages.developer.maintenance.type'), value: 'type' },
        { title: this.$t('pages.settings.subpages.developer.maintenance.message'), value: 'message' },
        { title: this.$t('pages.settings.subpages.developer.maintenance.action'), value: 'action' }
      ],
      items: [
        {
          title: this.$t('navigation.user'),
          to: '/user'
        },
        {
          title: this.$t('toolbars.user.developer'),
          to: `/user/developer`,
          disabled: true
        }
      ],
      messages: [],
      loadingMessages: false,
      dialog: false,
      messageId: null,
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    token () {
      return this.userStore.getToken
    },
    tokenExpiry () {
      if (!this.token) {
        return null
      }
      const authenticationService = useAuthenticationService()
      return formatTimestampUTCLabel(authenticationService.tokenToExpiryDate(this.token))
    },
    refreshToken () {
      return this.userStore.getRefreshToken
    },
    refreshTokenExpiry () {
      if (!this.refreshToken) {
        return null
      }
      const authenticationService = useAuthenticationService()
      return formatTimestampUTCLabel(authenticationService.tokenToExpiryDate(this.refreshToken))
    },
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    canCreateMessage () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-maintenance-message')
    },
    canModifyMessage () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('modify-maintenance-message')
    },
    canHandleMessages () {
      return this.canCreateMessage || this.canModifyMessage
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
  mounted () {
    this.loadMessages()
    if (!this.token || !this.refreshToken) {
      return
    }
    this.accessTokenField = this.token
    this.refreshTokenField = this.refreshToken
  },
  methods: {
    submit () {
    },
    modifyMessage (message) {
      this.messageId = message.id
      this.dialog = true
    },
    createMessage () {
      this.messageId = null
      this.dialog = true
    },
    expiryLabel (token) {
      const authenticationService = useAuthenticationService()
      return this.$t('pages.settings.subpages.developer.token.expiry') + ' ' + timestampsToHumanDifference(Date.now(), authenticationService.tokenToExpiryDate(token))
    },
    loadMessages () {
      const messageService = useMessageService()
      messageService.findAll()
        .then((messages) => {
          this.messages = messages.map((message) => {
            message.active = isActiveMessage(message) ? '● true' : 'false'
            return message
          })
        })
        .catch(() => {
          this.loadingMessages = false
        })
        .finally(() => {
          this.loadingMessages = false
        })
    },
    closeDialog (event) {
      if (event.success) {
        this.cacheStore.reloadMessages()
      }
      this.dialog = false
    }
  }
}
</script>
