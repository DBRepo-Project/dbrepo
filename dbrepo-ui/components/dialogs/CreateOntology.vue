<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card
        :title="$t('toolbars.semantic.register.title')"
        :subtitle="$t('toolbars.semantic.register.subtitle')"
        variant="elevated">
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-text-field
                id="prefix"
                v-model="createOntologyDto.prefix"
                name="prefix"
                label="Prefix *"
                hint="Only lowercase alphanumeric letters, max. 8"
                :variant="inputVariant"
                autofocus
                :rules="[
                  v => notEmpty(v) || $t('validation.required'),
                  v => validPrefix(v) || $t('validation.prefix.pattern'),
                  v => validPrefixLength(v,1,8) || $t('validation.prefix.length'),
                  v => !ontologies.map(o => o.prefix).includes(v) || $t('validation.prefix.exists')
                ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="uri"
                v-model="createOntologyDto.uri"
                name="uri"
                label="URI *"
                :variant="inputVariant"
                :rules="[
                  v => notEmpty(v) || $t('validation.required'),
                  v => validUri(v) || $t('validation.uri.pattern'),
                  v => !ontologies.map(o => o.uri).includes(v) || $t('validation.uri.exists')
                ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="sparql-endpoint"
                v-model="createOntologyDto.sparql_endpoint"
                :variant="inputVariant"
                name="sparql-endpoint"
                label="SPARQL Endpoint"
                :rules="[
                  v => validUriOptional(v) || $t('validation.uri.pattern')
                ]" />
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
            id="createDB"
            :disabled="!valid || loading"
            color="primary"
            variant="flat"
            type="submit"
            :loading="loading"
            :text="$t('navigation.create')"
            @click="create" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { notEmpty } from '@/utils'
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      valid: false,
      loading: false,
      createOntologyDto: {
        uri: null,
        prefix: null,
        sparql_endpoint: null
      },
      cacheStore: useCacheStore()
    }
  },
  computed: {
    ontologies () {
      return this.cacheStore.getOntologies
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
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
    create () {
      this.loading = true
      const ontologyService = useOntologyService()
      ontologyService.create(this.createOntologyDto)
        .then((ontology) => {
          this.$emit('close', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    validPrefix (str) {
      if (!str) {
        return false
      }
      return str.match(/[a-z0-9]+/g)
    },
    validPrefixLength (str, min, max) {
      if (!str) {
        return false
      }
      return str.length > min && str.length <= max
    },
    validUriOptional (str) {
      if (!str) {
        return true
      }
      return this.validUri(str)
    },
    validUri (str) {
      if (!str) {
        return false
      }
      return str.match(/^https?:\/\//g)
    },
    notEmpty
  }
}
</script>
