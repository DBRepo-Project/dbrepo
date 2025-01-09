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
          <v-row>
            <v-col>
              <v-textarea
                v-model="modify.description"
                rows="2"
                :rules="[
                  v => (!!v || v.length <= 180) || ($t('validation.max-length') + 180),
                ]"
                clearable
                counter="180"
                persistent-counter
                persistent-hint
                :variant="inputVariant"
                :hint="$t('pages.table.subpages.import.description.hint')"
                :label="$t('pages.table.subpages.import.description.label')"/>
            </v-col>
          </v-row>
          <v-row
            dense>
            <v-col
              md="6">
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
                :hint="$t('pages.database.resource.data.hint')" />
            </v-col>
            <v-col
              md="6">
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
                :hint="$t('pages.database.resource.schema.hint')" />
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
            :loading="loading"
            type="submit"
            :text="$t('navigation.modify')"
            @click="update" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'

export default {
  props: {
    table: {
      type: Object,
      default () {
        return {
          is_public: true,
          is_schema_public: true,
          description: null
        }
      }
    },
  },
  data () {
    return {
      valid: false,
      loading: false,
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
      modify: {
        description: this.table.description,
        is_public: this.table.is_public,
        is_schema_public: this.table.is_schema_public
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
      if (this.table.description !== this.modify.description) {
        return true
      }
      if (this.table.is_public !== this.modify.is_public) {
        return true
      }
      return this.table.is_schema_public !== this.modify.is_schema_public
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
    update () {
      this.loading = true
      const tableService = useTableService()
      tableService.update(this.$route.params.database_id, this.$route.params.table_id, this.modify)
        .then(() => {
          this.loading = false
          const toast = useToastInstance()
          toast.success(this.$t('success.table.updated'))
          this.$emit('close', { success: true })
          this.cacheStore.reloadTable()
        })
        .catch(({ code }) => {
          this.loading = false
          const toast = useToastInstance()
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
