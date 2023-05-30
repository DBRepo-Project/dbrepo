<template>
  <div>
    <v-card>
      <v-progress-linear
        v-if="loadingSemantics"
        color="primary"
        indeterminate />
      <v-card-title v-text="column.name" />
      <v-card-subtitle v-if="loadingSemantics && !semanticEntity">Loading semantic recommendations ...</v-card-subtitle>
      <v-card-text>
        <v-alert
          v-if="!entity"
          border="left"
          color="info"
          dark
          icon="mdi-share-variant"
          class="pl-6">
          <p>
            The following ontologies automatically will query the fields <code>rdfs:label</code> and store it for this
            column. You can still use other URIs that are not matching these ontologies, the URI will be displayed
            instead.
          </p>
          <ul>
            <li v-for="(item,idx) in ontologies" :key="idx">
              <a :href="item.uri" target="_blank" v-text="item.uri" />
            </li>
          </ul>
        </v-alert>
        <v-alert
          v-if="entity"
          border="left"
          color="primary"
          dark
          icon="mdi-share-variant"
          class="pl-6">
          <div>
            <a :href="entity.uri" class="white--text" target="_blank" v-text="entity.name ? entity.name : entity.uri" />
          </div>
          <div v-text="entity.description" />
        </v-alert>
      </v-card-text>
      <v-card-text>
        <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
          <v-list-item-group v-if="!loadingSemantics" v-model="recommendation">
            <v-list-item v-for="(item,idx) in recommendations" :key="idx" three-line>
              <template v-slot:default="{ active, }">
                <v-list-item-action>
                  <v-checkbox
                    :input-value="active"
                    color="primary" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title v-text="item.label" />
                  <v-list-item-subtitle v-text="item.uri" />
                  <v-list-item-subtitle class="mt-1" v-text="item.description" />
                </v-list-item-content>
              </template>
            </v-list-item>
          </v-list-item-group>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="uri"
                :loading="loading"
                :success="canAutomaticResolve"
                :hint="canAutomaticResolve ? 'This URI can be automatically resolved!' : 'e.g. http://www.wikidata.org/entity/Q468777'"
                :persistent-hint="canAutomaticResolve"
                clearable
                label="URI"
                :rules="[v => isUri(v) || $t('Must start with http:// or https://')]"
                @click:clear="uri = null" />
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          class="mb-2"
          @click="cancel">
          Cancel
        </v-btn>
        <v-btn
          color="primary"
          class="mb-2 mr-2"
          :disabled="!valid"
          :loading="loadingSave"
          @click="save">
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import TableService from '@/api/table.service'
import SemanticService from '@/api/semantic.service'

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
      loadingSemantics: false
    }
  },
  computed: {
    ontologies () {
      const ontologies = this.$store.state.ontologies
      if (!ontologies) {
        return []
      }
      return ontologies.filter(o => o.sparql)
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
    }
  },
  watch: {
    column () {
      if (!this.column[this.mode]) {
        this.recommendSemantics()
      }
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
    this.init()
    if (!this.column[this.mode]) {
      this.recommendSemantics()
    }
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
      TableService.updateColumn(this.database.id, this.database.id, this.tableId, this.column.id, payload)
        .then(() => {
          this.$emit('close', {
            success: true,
            action: 'assign'
          })
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    recommendSemantics () {
      this.loadingSemantics = true
      SemanticService.suggestTableColumn(this.database.id, this.tableId, this.column.id)
        .then((recommendations) => {
          this.recommendations = recommendations
        })
        .finally(() => {
          this.loadingSemantics = false
        })
    },
    isUri (str) {
      if (!str) {
        return true
      }
      return str.match(/https?:\/\//g)
    },
    init () {
      if (this.column.unit && this.mode === 'unit') {
        this.uri = this.column.unit.uri
        return
      }
      if (this.column.concept && this.mode === 'concept') {
        this.uri = this.column.concept.uri
        return
      }
      this.uri = null
    },
    submit () {
      this.$refs.form.validate()
    }
  }
}
</script>
<style scoped>
</style>
