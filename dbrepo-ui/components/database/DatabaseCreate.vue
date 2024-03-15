<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="submit">
      <v-card
        variant="elevated"
        :title="$t('pages.database.subpages.create.title')"
        :subtitle="$t('pages.database.subpages.create.subtitle')">
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="createDatabaseDto.name"
                name="database"
                :variant="inputVariant"
                :label="$t('pages.database.subpages.create.name.label')"
                :hint="$t('pages.database.subpages.create.name.hint')"
                persistent-hint
                :placeholder="$t('pages.database.subpages.create.name.placeholder')"
                autofocus
                :rules="[v => notEmpty(v) || $t('validation.required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                v-model="engine"
                name="engine"
                :label="$t('pages.database.subpages.create.engine.label')"
                :hint="$t('pages.database.subpages.create.engine.hint')"
                persistent-hint
                :variant="inputVariant"
                :items="engines"
                item-title="name"
                item-value="id"
                :rules="[v => !!v || $t('validation.required')]"
                return-object
                required />
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
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            variant="flat"
            :text="$t('pages.database.subpages.create.submit.text')"
            :loading="loading"
            @click="create" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { notEmpty } from '@/utils'

export default {
  data () {
    return {
      valid: false,
      loading: false,
      engine: {
        repository: null,
        tag: null
      },
      engines: [],
      createDatabaseDto: {
        name: null,
        is_public: true
      }
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
  mounted () {
    this.getEngines()
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
    getEngines () {
      this.loading = true
      const containerService = useContainerService()
      containerService.findAll()
        .then((containers) => {
          this.engines = containers
          if (this.engines.length > 0) {
            this.engine = this.engines[0]
          }
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    create () {
      this.loading = true
      const payload = { container_id: this.engine.id, name: this.createDatabaseDto.name, is_public: true }
      const databaseService = useDatabaseService()
      databaseService.create(payload)
        .then((database) => {
          this.loading = false
          this.$emit('close', { success: true, database_id: database.id })
        })
        .catch(() => {
          this.loading = false
          this.$toast.error(this.$t('errors.database.create'))
        })
        .finally(() => {
          this.loading = false
        })
    },
    notEmpty
  }
}
</script>
