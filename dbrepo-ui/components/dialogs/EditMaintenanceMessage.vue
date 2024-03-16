<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="submit">
      <v-card
        :title="$t('pages.settings.subpages.developer.maintenance.create.title')">
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-select
                v-model="localMessage.type"
                :items="types"
                item-title="name"
                item-value="value"
                :rules="[v => !!v || $t('validation.required')]"
                required
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.settings.subpages.developer.maintenance.create.type.label')"
                :hint="$t('pages.settings.subpages.developer.maintenance.create.type.hint')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="localMessage.message"
                :rules="[v => !!v || $t('validation.required')]"
                required
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.settings.subpages.developer.maintenance.create.message.label')"
                :hint="$t('pages.settings.subpages.developer.maintenance.create.message.hint')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-text-field
                v-model="localMessage.display_start"
                clearable
                :variant="inputVariant"
                hint="YYYY-MM-dd HH:mm:ss"
                persistent-hint
                :label="$t('pages.settings.subpages.developer.maintenance.create.start.label')" />
            </v-col>
            <v-col cols="6">
              <v-text-field
                v-model="localMessage.display_end"
                clearable
                :variant="inputVariant"
                hint="YYYY-MM-dd HH:mm:ss"
                :label="$t('pages.settings.subpages.developer.maintenance.create.end.label')" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            v-if="isModification"
            color="error"
            variant="flat"
            :text="$t('pages.settings.subpages.developer.maintenance.create.delete.text')"
            @click="deleteMessage" />
          <v-spacer />
          <v-btn
            :variant="buttonVariant"
            :text="$t('navigation.cancel')"
            @click="cancel" />
          <v-btn
            id="database"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            variant="flat"
            :text="$t('pages.settings.subpages.developer.maintenance.create.submit.text')"
            :loading="loading"
            @click="submitButton" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { timestampToTimeZonedTimestamp, formatTimestampUTC } from '@/utils'
import { useCacheStore } from '@/stores/cache'

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
      },
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    isModification () {
      return this.id !== null
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
      const messageService = useMessageService()
      messageService.findOne(id)
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
      const messageService = useMessageService()
      messageService.create(payload)
        .then(() => {
          this.$emit('close-dialog', { success: true })
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
      const messageService = useMessageService()
      messageService.update(this.localMessage.id, payload)
        .then(() => {
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    deleteMessage () {
      this.loading = true
      const messageService = useMessageService()
      messageService.remove(this.localMessage.id)
        .then(() => {
          this.$emit('close-dialog', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
