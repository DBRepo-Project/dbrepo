<template>
  <div>
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="canHandleMessages" flat tile>
          <v-card-title>Maintenance Messages</v-card-title>
          <v-data-table
            :headers="headers"
            :items="messages"
            :loading="loadingMessages"
            :items-per-page="10">
            <template v-slot:item.action="{ item }">
              <v-btn
                x-small
                @click="modifyMessage(item)">
                Modify
              </v-btn>
            </template>
          </v-data-table>
          <v-card-text>
            <v-btn
              small
              color="secondary"
              :disabled="!canCreateMessage"
              @click="createMessage">
              Create Message
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider v-if="canHandleMessages" />
        <v-card flat tile>
          <v-card-title>Token Information</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col xl="4">
                <v-text-field
                  v-model="token"
                  disabled
                  label="Access Token" />
              </v-col>
              <v-col xl="2">
                <v-text-field
                  v-model="tokenExpiry"
                  disabled
                  :label="tokenExpiryLabel" />
              </v-col>
            </v-row>
            <v-row dense>
              <v-col xl="4">
                <v-text-field
                  v-model="refreshToken"
                  disabled
                  label="Refresh Token" />
              </v-col>
              <v-col xl="2">
                <v-text-field
                  v-model="refreshTokenExpiry"
                  disabled
                  :label="refreshTokenExpiryLabel" />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-dialog
      v-model="dialog"
      persistent
      max-width="640">
      <EditMaintenanceMessage :id="messageId" @close-dialog="closeDialog" />
    </v-dialog>
  </div>
</template>

<script>
import UserToolbar from '@/components/UserToolbar'
import MetadataService from '@/api/metadata.service'
import EditMaintenanceMessage from '@/components/dialogs/EditMaintenanceMessage'
import { formatTimestampUTCLabel, isActiveMessage, timestampsToHumanDifference } from '@/utils'
import AuthenticationMapper from '@/api/authentication.mapper'

export default {
  components: {
    UserToolbar,
    EditMaintenanceMessage
  },
  data () {
    return {
      tab: 0,
      headers: [
        { text: 'Active', value: 'active' },
        { text: 'Type', value: 'type' },
        { text: 'Message', value: 'message' },
        { text: 'Action', value: 'action' }
      ],
      messages: [],
      loadingMessages: false,
      dialog: false,
      messageId: null
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    tokenExpiry () {
      if (!this.token) {
        return null
      }
      return formatTimestampUTCLabel(AuthenticationMapper.tokenToExpiryDate(this.token))
    },
    tokenExpiryLabel () {
      if (!this.token) {
        return 'Expiry Date'
      }
      return `Expiry Date (${timestampsToHumanDifference(Date.now(), AuthenticationMapper.tokenToExpiryDate(this.token))})`
    },
    refreshToken () {
      return this.$store.state.refreshToken
    },
    refreshTokenExpiry () {
      if (!this.refreshToken) {
        return null
      }
      return formatTimestampUTCLabel(AuthenticationMapper.tokenToExpiryDate(this.refreshToken))
    },
    refreshTokenExpiryLabel () {
      if (!this.refreshToken) {
        return 'Expiry Date'
      }
      return `Expiry Date (${timestampsToHumanDifference(Date.now(), AuthenticationMapper.tokenToExpiryDate(this.refreshToken))})`
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
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
    }
  },
  mounted () {
    this.loadMessages()
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
    loadMessages () {
      MetadataService.findAllMessages()
        .then((messages) => {
          this.messages = messages.map((message) => {
            message.active = isActiveMessage(message) ? '● true' : 'false'
            message.action = 'hello'
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
      this.dialog = false
      if (event.success) {
        this.loadMessages()
        this.$store.dispatch('reloadMessages')
      }
    }
  }
}
</script>
