<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
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
                label="Start timestamp" />
            </v-col>
            <v-col cols="6">
              <v-text-field
                v-model="localMessage.display_end"
                clearable
                label="End timestamp" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="localMessage.link"
                clearable
                label="Link" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="localMessage.link_text"
                clearable
                label="Link Text" />
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
        display_end: null,
        link: null,
        link_text: null
      },
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
          display_end: null,
          link: null,
          link_text: null
        }
      } else {
        this.loadMessage(this.id)
      }
    },
    loadMessage (id) {
      MetadataService.findMessage(id)
        .then((message) => {
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
      MetadataService.createMessage(this.localMessage)
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
