<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-card-title v-text="title" />
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-select
                v-model="localMessage.type"
                :items="types"
                item-text="name"
                item-value="value"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Type *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="localMessage.message"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Message *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-text-field
                v-model="localMessage.display_start"
                clearable
                hint="YYYY-MM-dd HH:mm:ss"
                label="Start timestamp" />
            </v-col>
            <v-col cols="6">
              <v-text-field
                v-model="localMessage.display_end"
                clearable
                hint="YYYY-MM-dd HH:mm:ss"
                label="End timestamp" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            v-if="isModification"
            class="ml-2"
            color="error"
            @click="deleteMessage">
            Delete
          </v-btn>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            id="database"
            class="mb-2 ml-3 mr-2"
            :disabled="!valid || loading"
            :color="buttonColor"
            type="submit"
            :loading="loading"
            @click="submitButton">
            {{ buttonText }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import MetadataService from '@/api/metadata.service'
import { timestampToTimeZonedTimestamp, formatTimestampUTC } from '@/utils'

export default {
  props: {
    id: {
      type: Number,
      default () {
        return null
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      types: [
        { name: 'Error', value: 'error' },
        { name: 'Warning', value: 'warning' },
        { name: 'Info', value: 'info' }
      ],
      localMessage: {
        type: null,
        message: null,
        display_start: null,
        display_end: null
      },
      modify: {
        username: null,
        type: null
      }
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    title () {
      return (!this.isModification ? 'Create' : 'Modify') + ' maintenance message'
    },
    buttonColor () {
      if (this.modify.type && this.modify.type === 'revoke') {
        return 'error'
      }
      return 'secondary'
    },
    isModification () {
      return this.id !== null
    },
    buttonText () {
      return (this.isModification ? 'Modify' : 'Create') + ' message'
    }
  },
  watch: {
    id () {
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
    init () {
      if (!this.id) {
        this.localMessage = {
          type: null,
          message: null,
          display_start: null,
          display_end: null
        }
      } else {
        this.loadMessage(this.id)
      }
    },
    loadMessage (id) {
      MetadataService.findMessage(id)
        .then((message) => {
          message.display_start = formatTimestampUTC(message.display_start)
          message.display_end = formatTimestampUTC(message.display_end)
          this.localMessage = message
        })
    },
    submitButton () {
      if (this.isModification) {
        this.updateMessage()
      } else {
        this.createMessage()
      }
    },
    createMessage () {
      this.loading = true
      const payload = Object.assign({}, this.localMessage)
      if (payload.display_start) {
        payload.display_start = timestampToTimeZonedTimestamp(payload.display_start)
      }
      if (payload.display_end) {
        payload.display_end = timestampToTimeZonedTimestamp(payload.display_end)
      }
      MetadataService.createMessage(payload)
        .then(() => {
          this.$emit('close-dialog', { success: true })
          this.$emit('reload-messages', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    updateMessage () {
      this.loading = true
      const payload = Object.assign({}, this.localMessage)
      delete payload.id
      if (payload.display_start) {
        payload.display_start = timestampToTimeZonedTimestamp(payload.display_start)
      }
      if (payload.display_end) {
        payload.display_end = timestampToTimeZonedTimestamp(payload.display_end)
      }
      MetadataService.updateMessage(this.localMessage.id, payload)
        .then(() => {
          this.$emit('close-dialog', { success: true })
          this.$emit('reload-messages', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    deleteMessage () {
      this.loading = true
      MetadataService.deleteMessage(this.localMessage.id)
        .then(() => {
          this.$emit('close-dialog', { success: true })
          this.$emit('reload-messages', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
