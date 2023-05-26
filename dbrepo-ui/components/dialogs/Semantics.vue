<template>
  <div>
    <v-card>
      <v-card-title>Assign Semantic Information</v-card-title>
      <v-card-subtitle v-if="!loadingSemantics">
        Recommend <strong v-text="recommendations.length" /> {{ recommendations.length === 1 ? 'entity' : 'entities' }} that matches the column name
      </v-card-subtitle>
      <v-card-text>
        <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
          <v-row>
            <v-col>
              <v-progress-linear
                v-if="loadingSemantics"
                color="secondary"
                indeterminate />
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
                      <v-list-item-subtitle class="mt-1" v-text="item.comment" />
                    </v-list-item-content>
                  </template>
                </v-list-item>
              </v-list-item-group>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="uri"
                :loading="loading"
                clearable
                label="URI"
                :rules="[v => isUri(v) || $t('Must start with http:// or https://')]"
                hint="e.g. http://www.wikidata.org/entity/Q468777"
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
      loadingSemantics: false,
      ontologies: []
    }
  },
  computed: {
  },
  watch: {
    column () {
      this.loadSemantics()
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
    recommendation (index) {
      if (!this.recommendations[index] || !('uri' in this.recommendations[index])) {
        this.uri = null
        return
      }
      this.uri = this.recommendations[index].uri
    }
  },
  mounted () {
    this.loadOntologies()
    this.loadSemantics()
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
    loadOntologies () {
      this.loadingOntologies = true
      SemanticService.findAllOntologies()
        .then((ontologies) => {
          this.ontologies = ontologies
        })
        .finally(() => {
          this.loadingOntologies = false
        })
    },
    loadSemantics () {
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
    submit () {
      this.$refs.form.validate()
    }
  }
}
</script>
<style scoped>
</style>
