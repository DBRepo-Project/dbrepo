<template>
  <div>
    <v-card
      :title="$t('pages.table.subpages.versioning.title')"
      :subtitle="$t('pages.table.subpages.versioning.subtitle')"
      variant="elevated">
      <v-progress-linear v-if="loading" color="primary" />
      <v-card-text>
        <v-text-field
          v-model="datetime"
          required
          :rules="[
            v => !!v || $t('validation.required'),
            v => v && /^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$/.test(v) || $t('validation.pattern.timestamp')]"
          persistent-hint
          :variant="inputVariant"
          :label="$t('pages.table.subpages.versioning.timestamp.label')"
          :hint="$t('pages.table.subpages.versioning.timestamp.hint')"
          :placeholder="$t('pages.table.subpages.versioning.timestamp.placeholder')"
          :suffix="$t('pages.table.subpages.versioning.timestamp.suffix')"
          class="mb-4"
          type="text" />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          :variant="buttonVariant"
          :text="$t('navigation.cancel')"
          @click="cancel" />
        <v-btn
          color="tertiary"
          variant="flat"
          :text="$t('navigation.now')"
          @click="reset" />
        <v-btn
          color="primary"
          variant="flat"
          :disabled="datetime === null || datetime === undefined || datetime === ''"
          :text="$t('navigation.continue')"
          @click="pick" />
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      formValid: false,
      loading: false,
      datetime: null,
      totalChanges: 0
    }
  },
  computed: {
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  methods: {
    cancel () {
      this.$emit('close', { success: false })
    },
    reset () {
      this.$emit('close', { success: true, timestamp: null })
    },
    pick () {
      this.$emit('close', {
        success: true,
        timestamp: this.datetime
      })
    }
  }
}
</script>
