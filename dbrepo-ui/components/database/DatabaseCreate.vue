<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="submit">
      <v-card
        variant="elevated">
        <v-card-title>
          {{ $t('pages.database.subpages.create.title') }}
        </v-card-title>
        <v-card-subtitle>
          {{ $t('pages.database.subpages.create.subtitle') }}
        </v-card-subtitle>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="payload.name"
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
                :loading="loadingContainers"
                item-title="name"
                item-value="id"
                :rules="[v => !!v || $t('validation.required')]"
                return-object
                required>
                <template
                  v-if="engine"
                  v-slot:details>
                  {{ $t('pages.database.subpages.create.utilization.label') }} {{ engine.count }}/{{ engine.quota }}
                </template>
              </v-select>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-select
                v-model="mode"
                name="mode"
                :label="$t('pages.database.subpages.create.visibility.label')"
                :hint="$t('pages.database.subpages.create.visibility.hint')"
                persistent-hint
                :variant="inputVariant"
                :items="visibilityOptions"
                item-title="name"
                item-value="value"
                :rules="[v => !!v || $t('validation.required')]"
                return-object
                required>
                <template
                  v-slot:append-inner>
                  <v-tooltip
                    location="bottom">
                    <template
                      v-slot:activator="{ props }">
                      <v-icon
                        v-bind="props"
                        icon="mdi-help-circle-outline" />
                    </template>
                    {{ mode.hint }}
                  </v-tooltip>
                </template>
              </v-select>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            :variant="buttonVariant"
            @click="cancel">
            {{ $t('navigation.cancel') }}
          </v-btn>
          <v-btn
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            variant="flat"
            :loading="loading"
            @click="create">
            {{ $t('pages.database.subpages.create.submit.text') }}
          </v-btn>
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
      loadingContainers: false,
      engine: null,
      engines: [],
      visibilityOptions: [
        {
          name: this.$t('pages.database.subpages.create.visibility.public.label'),
          hint: this.$t('pages.database.subpages.create.visibility.public.hint'),
          value: true
        },
        {
          name: this.$t('pages.database.subpages.create.visibility.private.label'),
          hint: this.$t('pages.database.subpages.create.visibility.private.hint'),
          value: false
        }
      ],
      mode: true,
      payload: {
        name: null,
        is_public: true,
        is_schema_public: true,
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
    this.mode = this.visibilityOptions[0]
    this.fetchContainers()
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
    fetchContainers () {
      const containerService = useContainerService()
      this.loadingContainers = true
      containerService.findAll()
        .then((containers) => {
          const freeContainers = containers.filter(c => c.count < c.quota)
          const defaultContainers = freeContainers.filter(c => c.image.default)
          defaultContainers.sort(this.compareContainerUtilization)
          this.engines = defaultContainers
          const other = freeContainers.filter(c => !c.image.default)
          other.sort(this.compareContainerUtilization)
          other.forEach(c => this.engines.push(c))
          if (this.engines.length > 0) {
            this.engine = this.engines[0]
          }
          this.loadingContainers = false
        })
        .catch(({code}) => {
          this.loadingContainers = false
          const toast = useToastInstance()
          toast.error(this.$t(code))
        })
    },
    create () {
      this.loading = true
      this.payload.container_id = this.engine.id
      this.payload.is_public = this.mode.value
      this.payload.is_schema_public = this.mode.value
      const databaseService = useDatabaseService()
      databaseService.create(this.payload)
        .then(async (database) => {
          await this.$router.push(`/database/${database.id}/info`)
          this.loading = false
        })
        .catch(({code}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
    },
    compareContainerUtilization (container, other) {
      return Math.round(container.count / container.quota) < Math.round(other.count / other.quota)
    },
    notEmpty
  }
}
</script>
