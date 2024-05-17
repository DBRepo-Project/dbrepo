<template>
  <div>
    <v-card
      :title="title"
      :subtitle="$t('pages.table.subpages.semantics.subtitle')"
      variant="elevated">
      <v-card-text class="pb-0">
        <v-row
          v-if="!entity"
          dense>
          <v-col>
            <v-alert
              border="start"
              color="info">
              <p
                v-text="$t('pages.table.subpages.semantics.info')" />
              <p
                class="mt-1"
                v-for="(ontology, idx) in ontologies"
                :key="`o-${idx}`">
                <v-badge inline :content="badge(ontology).text" :color="badge(ontology).color">
                  <a :href="ontology.uri" v-text="ontology.uri_pattern" />
                </v-badge>
              </p>
            </v-alert>
          </v-col>
        </v-row>
        <v-row
          v-else
          dense>
          <v-col>
            <v-alert
              border="start"
              color="info">
              <p>
                <a
                  :href="entity.uri"
                  v-text="entity.name ? entity.name : entity.uri" />
              </p>
              <p
                v-text="entity.description" />
            </v-alert>
          </v-col>
        </v-row>
        <v-row
          v-if="recommendations.length === 0">
          <v-col>
            <v-btn
              color="secondary"
              variant="flat"
              size="small"
              :text="$t('navigation.recommend')"
              :loading="loadingSemantics"
              @click="recommendSemantics" />
          </v-col>
        </v-row>
        <v-form
          ref="form"
          v-model="valid"
          @submit.prevent="submit">
          <v-row
            v-if="recommendations.length > 0">
            <v-col>
              <v-list
                lines="one"
                v-model="recommendation"
                select-strategy="single-independent">
                <v-list-subheader
                  v-text="$t('pages.table.subpages.semantics.recommended')" />
                <v-list-item
                  v-for="(item, idx) in recommendations"
                  :key="`r-${idx}`"
                  :value="item.uri"
                  @click="uri = item.uri">
                  <template v-slot:prepend="{ isActive }">
                    <v-list-item-action start>
                      <v-checkbox-btn :model-value="isActive"></v-checkbox-btn>
                    </v-list-item-action>
                  </template>
                  <v-list-item-title v-text="item.label" />
                  <v-list-item-subtitle v-text="subtitle(item)" />
                </v-list-item>
              </v-list>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="uri"
                :loading="loading"
                :success="canAutomaticResolve"
                :persistent-hint="canAutomaticResolve"
                clearable
                persistent-hint
                :variant="inputVariant"
                :label="$t('pages.table.subpages.semantics.uri.label')"
                :hint="canAutomaticResolve ? $t('pages.table.subpages.semantics.uri.hint') : ''"
                :rules="[v => isUri(v) || $t('validation.uri.pattern')]"
                @click:clear="uri = null" />
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          :variant="buttonVariant"
          :text="$t('navigation.cancel')"
          @click="cancel" />
        <v-btn
          color="primary"
          variant="flat"
          :text="$t('navigation.assign')"
          :disabled="!valid"
          :loading="loadingSave"
          @click="save" />
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'

export default {
  props: {
    column: {
      type: Object,
      default: () => ({})
    },
    database: {
      type: Object,
      default: () => null
    },
    tableId: {
      type: Number,
      default: () => -1
    },
    mode: {
      type: String,
      default: () => 'concept'
    }
  },
  data () {
    return {
      recommendation: null,
      recommendations: [],
      dialog: false,
      saved: false,
      loadingSave: false,
      uri: null,
      valid: false,
      loading: false,
      loadingOntologies: false,
      loadingSemantics: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    title () {
      return this.$t('pages.table.subpages.semantics.title') + ' ' +  this.column.internal_name
    },
    ontologies () {
      return this.cacheStore.getOntologies.filter(o => o.sparql || o.rdf)
    },
    canAutomaticResolve () {
      if (!this.uri) {
        return false
      }
      let found = false
      this.ontologies.forEach((o) => {
        if (this.uri.startsWith(o.uri)) {
          found = true
        }
      })
      return found
    },
    entity () {
      if (!this.column[this.mode]) {
        return null
      }
      return this.column[this.mode]
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
    column () {
      this.recommendations = []
      this.init()
    },
    recommendation (index) {
      if (!this.recommendations[index] || !('uri' in this.recommendations[index])) {
        this.uri = null
        return
      }
      this.uri = this.recommendations[index].uri
    }
  },
  mounted () {
    this.recommendations = []
    this.init()
  },
  methods: {
    cancel () {
      this.$emit('close', { success: false, action: 'cancel' })
    },
    save () {
      const conceptUri = this.column.concept ? this.column.concept.uri : null
      const unitUri = this.column.unit ? this.column.unit.uri : null
      const payload = {
        concept_uri: this.mode === 'concept' ? this.uri : conceptUri,
        unit_uri: this.mode === 'unit' ? this.uri : unitUri
      }
      this.loadingSave = true
      const tableService = useTableService()
      tableService.update(this.database.id, this.tableId, this.column.id, payload)
        .then(() => {
          this.recommendation = null
          this.$refs.form.reset()
          this.$emit('close', {
            success: true,
            action: 'assign'
          })
        })
        .finally(() => {
          this.recommendation = null
          this.$refs.form.reset()
          this.loadingSave = false
        })
    },
    recommendSemantics () {
      this.loadingSemantics = true
      const tableService = useTableService()
      tableService.suggest(this.database.id, this.tableId, this.column.id)
        .then((recommendations) => {
          this.recommendations = recommendations
        })
        .catch((error) => {
          this.$toast.error(this.$t('error.semantics.timeout'))
        })
        .finally(() => {
          this.loadingSemantics = false
        })
    },
    isUri (str) {
      if (!str) {
        return true
      }
      return str.startsWith('http')
    },
    badge (ontology) {
      if (ontology.sparql) {
        return { color: 'success', text: 'SPARQL' }
      }
      if (ontology.rdf) {
        return { color: 'secondary', text: 'RDF' }
      }
      return null
    },
    subtitle (entity) {
      if (entity.description) {
        return `${entity.description} ${this.$t('pages.table.subpages.semantics.bullet')} ${entity.uri}`
      }
      return entity.uri
    },
    init () {
      this.cacheStore.reloadOntologies()
      this.uri = null
      if (this.column.unit && this.mode === 'unit') {
        this.uri = this.column.unit.uri
        return
      }
      if (this.column.concept && this.mode === 'concept') {
        this.uri = this.column.concept.uri
      }
    },
    submit () {
      this.$refs.form.validate()
    }
  }
}
</script>
