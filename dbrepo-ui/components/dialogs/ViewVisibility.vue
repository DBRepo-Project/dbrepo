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
              md="6">
              <v-select
                v-model="modify.is_public"
                :items="visibilities"
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.subpages.create.data.label')"
                :hint="$t('pages.database.subpages.create.data.hint')" />
            </v-col>
            <v-col
              md="6">
              <v-select
                v-model="modify.is_schema_public"
                :items="visibilities"
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.subpages.create.schema.label')"
                :hint="$t('pages.database.subpages.create.schema.hint')" />
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
import { useCacheStore } from '@/stores/cache'

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
      visibilities: [
        { title: this.$t('toolbars.database.public'), value: true },
        { title: this.$t('toolbars.database.private'), value: false },
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
    updateVisibility () {
      this.loading = true
      const viewService = useViewService()
      viewService.update(this.$route.params.database_id, this.$route.params.view_id, this.modify)
        .then(() => {
          this.loading = false
          const toast = useToastInstance()
          toast.success(this.$t('success.view.modified'))
          this.$emit('close', { success: true })
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          toast.error(message)
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
