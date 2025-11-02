<template>
  <div>
    <v-card
      variant="elevated">
      <v-card-title>
        <span>{{ title }}</span>&nbsp;
        <code>{{ column.internal_name }}</code>
      </v-card-title>
      <v-card-subtitle
        v-text="$t('pages.table.subpages.semantics.subtitle')" />
      <v-card-text class="pb-0">
        <v-row
          v-if="!entity"
          dense>
          <v-col>
            <v-alert
              border="start"
              color="info">
              <p>
                {{ $t('pages.table.subpages.semantics.info') }}
              </p>
              <p
                class="mt-1"
                v-for="(ontology, idx) in ontologies"
                :key="`o-${idx}`">
                <v-badge
                  inline
                  :content="badge(ontology).text"
                  :color="badge(ontology).color">
                  <a
                    :href="ontology.uri">
                    {{ ontology.uri_pattern }}
                  </a>
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
                  :href="entity.uri">
                  {{ entity.name ? entity.name : entity.uri }}
                </a>
              </p>
              <p>
                {{ entity.description }}
              </p>
            </v-alert>
          </v-col>
        </v-row>
        <v-row
          v-if="recommendations.length === 0">
          <v-col>
            <v-btn
              v-if="finishedRecommendations"
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
                <v-list-subheader>
                  {{ $t('pages.table.subpages.semantics.recommended') }}
                </v-list-subheader>
                <v-list-item
                  v-for="(item, idx) in recommendations"
                  :key="`r-${idx}`"
                  :value="item.uri"
                  @click="uri = item.uri">
                  <template v-slot:prepend="{ isActive }">
                    <v-list-item-action start>
                      <v-checkbox-btn
                        :model-value="isActive" />
                    </v-list-item-action>
                  </template>
                  <v-list-item-title>
                    {{ item.label }}
                  </v-list-item-title>
                  <v-list-item-subtitle>
                    {{ subtitle(item) }}
                  </v-list-item-subtitle>
                </v-list-item>
              </v-list>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="concept.uri"
                :loading="loading"
                :success="canAutomaticResolve(concept)"
                :persistent-hint="canAutomaticResolve(concept)"
                clearable
                :variant="inputVariant"
                :label="$t('pages.table.subpages.semantics.concept.label')"
                :hint="canAutomaticResolve(concept) ? $t('pages.table.subpages.semantics.concept.hint') : ''"
                :rules="[v => isUri(v) || $t('validation.uri.pattern')]"
                @click:clear="concept.uri = null">
                <template
                  v-if="canAutomaticResolve(concept)"
                  v-slot:append-inner>
                  <v-icon
                    color="success">
                    mdi-check-circle-outline
                  </v-icon>
                </template>
              </v-text-field>
            </v-col>
            <v-col>
              <v-text-field
                v-model="unit.uri"
                :loading="loading"
                :success="canAutomaticResolve(unit)"
                :persistent-hint="canAutomaticResolve(unit)"
                clearable
                :variant="inputVariant"
                :label="$t('pages.table.subpages.semantics.unit.label')"
                :hint="canAutomaticResolve(unit) ? $t('pages.table.subpages.semantics.unit.hint') : ''"
                :rules="[v => isUri(v) || $t('validation.uri.pattern')]"
                @click:clear="unit.uri = null">
                <template
                  v-if="canAutomaticResolve(unit)"
                  v-slot:append-inner>
                  <v-icon
                    color="success">
                    mdi-check-circle-outline
                  </v-icon>
                </template>
              </v-text-field>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="description"
                :loading="loading"
                clearable
                persistent-hint
                :variant="inputVariant"
                :label="$t('pages.table.subpages.semantics.description.label')"
                :hint="$t('pages.table.subpages.semantics.description.hint')"
                @click:clear="description = null" />
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
import { useCacheStore } from '@/stores/cache.js'

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
      type: String,
      default: () => null
    }
  },
  data () {
    return {
      recommendation: null,
      recommendations: [],
      dialog: false,
      saved: false,
      loadingSave: false,
      concept: {
        uri: null
      },
      unit: {
        uri: null
      },
      description: null,
      valid: false,
      loading: false,
      finishedRecommendations: false,
      loadingOntologies: false,
      loadingSemantics: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    title () {
      return this.$t('pages.table.subpages.semantics.title')
    },
    ontologies () {
      return this.cacheStore.getOntologies.filter(o => o.sparql || o.rdf)
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
      const payload = {
        concept_uri: this.concept.uri,
        unit_uri: this.unit.uri,
        description: this.description,
      }
      this.loadingSave = true
      const tableService = useTableService()
      tableService.updateSemantics(this.database.id, this.tableId, this.column.id, payload)
        .then(() => {
          this.recommendation = null
          this.$refs.form.reset()
          this.$emit('close', {
            success: true,
            action: 'assign'
          })
        })
        .catch(({code, message}) => {
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
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
          this.finishedRecommendations = true
        })
        .catch(({code, message}) => {
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
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
      this.concept.uri = null
      this.unit.uri = null
      this.description = null
      if (this.column.unit) {
        this.unit.uri = this.column.unit.uri
      }
      if (this.column.concept) {
        this.concept.uri = this.column.concept.uri
      }
      if (this.column.description) {
        this.description = this.column.description
      }
    },
    submit () {
      this.$refs.form.validate()
    },
    canAutomaticResolve (item) {
      if (!item.uri) {
        return false
      }
      let found = false
      this.ontologies.forEach((o) => {
        if (item.uri.startsWith(o.uri)) {
          found = true
        }
      })
      return found
    },
  }
}
</script>
