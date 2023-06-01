<template>
  <div v-if="canHandleMessages">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
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
import { isActiveMessage } from '@/utils'

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
