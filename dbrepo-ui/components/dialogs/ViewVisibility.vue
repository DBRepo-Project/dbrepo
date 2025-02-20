<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="submit">
      <v-card
        :title="$t('pages.view.visibility.title')">
        <v-card-text>
          <v-row
            dense>
            <v-col
              lg="6">
              <v-select
                v-model="modify.is_public"
                :items="dataOptions"
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.data.label')"
                :hint="$t('pages.database.resource.data.hint', { resource: 'view' })" />
            </v-col>
            <v-col
              lg="6">
              <v-select
                v-model="modify.is_schema_public"
                :items="schemaOptions"
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.schema.label')"
                :hint="$t('pages.database.resource.schema.hint', { resource: 'view', schema: 'columns' })" />
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
            :disabled="!valid || !isChange"
            :color="buttonColor"
            type="submit"
            :text="$t('navigation.modify')"
            @click="updateVisibility" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'

export default {
  props: {
    view: {
      type: Object,
      default () {
        return {
          is_public: true,
          is_schema_public: true
        }
      }
    },
  },
  data () {
    return {
      valid: false,
      loading: false,
      loadingUsers: false,
      users: [],
      error: false,
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
      modify: {
        is_public: this.view.is_public,
        is_schema_public: this.view.is_schema_public
      },
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    isChange () {
      if (this.view.is_public !== this.modify.is_public) {
        return true
      }
      return  this.view.is_schema_public !== this.modify.is_schema_public
    },
    buttonColor () {
      return !this.isChange ? null : 'warning'
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
  }
}
</script>
