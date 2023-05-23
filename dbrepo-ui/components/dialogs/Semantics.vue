<template>
  <div>
    <v-card>
      <v-card-title>Assign Semantic Information</v-card-title>
      <v-card-subtitle>We recommend the following ontologies</v-card-subtitle>
      <v-card-text>
        <div v-for="(ontology,idx) in ontologies" :key="idx">
          <strong>{{ ontology.prefix }}</strong>: <a :href="ontology.uri" target="_blank">{{ ontology.uri }}</a>
        </div>
      </v-card-text>
      <v-card-text>
        <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
          <v-toolbar rounded outlined flat dense>
            <v-text-field
              v-model="uri"
              :loading="loading"
              solo
              flat
              dense
              clearable
              single-line
              hide-details
              :rules="[v => !!v || $t('Required')]"
              placeholder="http://www.wikidata.org/entity/Q468777"
              @click:clear="uri = null" />
          </v-toolbar>
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
      dialog: false,
      saved: false,
      loadingSave: false,
      uri: null,
      valid: false,
      loading: false,
      loadingOntologies: false,
      ontologies: []
    }
  },
  computed: {
  },
  watch: {
  },
  mounted () {
    this.loadOntologies()
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
        .catch(() => {
          this.loadingSave = true
        })
        .finally(() => {
          this.loadingSave = true
        })
    },
    loadOntologies () {
      this.loadingOntologies = true
      SemanticService.findAllOntologies()
        .then((ontologies) => {
          this.ontologies = ontologies
        })
        .finally(() => {
          this.loadingOntologies = true
        })
    },
    submit () {
      this.$refs.form.validate()
    }
  }
}
</script>
<style scoped>
</style>
