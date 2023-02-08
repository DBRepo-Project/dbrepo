<template>
  <div>
    <v-card>
      <v-card-title>
        Assign Semantic Information
      </v-card-title>
      <v-card-subtitle>
        Assign a <a href="https://www.wikidata.org/wiki/" target="_blank">Wikidata Semantic Concept</a> or
        <a href="http://www.ontology-of-units-of-measure.org/resource/om-2/Unit" target="_blank">Unit of Measurement</a>
        to column <strong>{{ column.name }}</strong>
      </v-card-subtitle>
      <v-card-text>
        <v-form ref="form" v-model="validConcept" autocomplete="off" @submit.prevent="submit">
          <p>Wikidata Semantic Concept</p>
          <v-toolbar rounded outlined flat dense>
            <v-text-field
              v-model="searchConcept"
              :loading="loadingConcept"
              solo
              flat
              dense
              clearable
              single-line
              hide-details
              placeholder="Search or URI (e.g., https://www.wikidata.org/entity/Q468777)"
              @click:clear="concept = null" />
            <v-btn v-if="!searchConceptContainsUri" icon class="ml-2" type="submit" @click="retrieveConcepts">
              <v-icon>mdi-magnify</v-icon>
            </v-btn>
          </v-toolbar>
          <div v-if="!validConcept" class="mt-1 error--text">Invalid URI! Valid URI is e.g., https://www.wikidata.org/entity/Q468777</div>
        </v-form>
        <v-list-item-group v-if="concepts.length > 0" v-model="listConcept" @change="selectConcept">
          <v-virtual-scroll
            :items="concepts"
            height="128"
            item-height="64">
            <template v-slot:default="{ item }">
              <v-list-item :key="`concept-${item}`" two-line>
                <v-list-item-content>
                  <v-list-item-title v-text="`${item.name} (${item.uri})`" />
                  <v-list-item-subtitle v-text="item.comment" />
                </v-list-item-content>
              </v-list-item>
            </template>
          </v-virtual-scroll>
        </v-list-item-group>
        <v-form ref="form" v-model="validUnit" autocomplete="off" @submit.prevent="submit">
          <p class="mt-4">Unit of Measurement</p>
          <v-toolbar rounded outlined flat dense>
            <v-text-field
              v-model="searchUnit"
              :loading="loadingUnit"
              solo
              flat
              dense
              clearable
              single-line
              hide-details
              placeholder="Search or provide URI..."
              @click:clear="unit = null" />
            <v-btn v-if="!searchUnitContainsUri" icon class="ml-2" type="submit" @click="retrieveUnits">
              <v-icon>mdi-magnify</v-icon>
            </v-btn>
          </v-toolbar>
        </v-form>
        <v-list-item-group v-if="units.length > 0" v-model="listUnit" @change="selectUnit">
          <v-virtual-scroll
            :items="units"
            height="128"
            item-height="64">
            <template v-slot:default="{ item }">
              <v-list-item :key="`unit-${item}`" two-line>
                <v-list-item-content>
                  <v-list-item-title v-text="item.name" />
                  <v-list-item-subtitle v-text="item.comment" />
                </v-list-item-content>
              </v-list-item>
            </template>
          </v-virtual-scroll>
        </v-list-item-group>
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
          :disabled="!validConcept || !validUnit"
          :loading="loadingSave"
          @click="save">
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
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
    }
  },
  data () {
    return {
      dialog: false,
      saved: false,
      loadingSave: false,
      validConcept: false,
      validUnit: false,
      loadingConcept: false,
      loadingUnit: false,
      listConcept: null,
      listUnit: null,
      selected: false,
      searchConcept: null,
      searchUnit: null,
      concepts: [],
      units: [],
      concept: {
        uri: null,
        name: null,
        symbol: null,
        comment: null
      },
      unit: {
        uri: null,
        name: null,
        symbol: null,
        comment: null
      },
      entries: []
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    name () {
      return this.saved && this.model && this.model.name
    },
    items () {
      return this.entries && this.entries.map((entry) => {
        return {
          text: entry.name,
          value: entry
        }
      })
    },
    searchUnitContainsUri () {
      if (!this.searchUnit) {
        return false
      }
      return this.searchUnit.startsWith('http')
    },
    searchConceptContainsUri () {
      if (!this.searchConcept) {
        return false
      }
      return this.searchConcept.startsWith('http')
    }
  },
  watch: {
    column () {
      this.reset()
    }
  },
  mounted () {
    this.reset()
  },
  methods: {
    cancel () {
      this.$emit('close', { success: false, action: 'cancel' })
    },
    selectUnit (idx) {
      if (idx === null || !this.units[idx]) {
        console.warn('select unit does not work', idx, this.units[idx])
        console.debug('units are', this.units)
        return
      }
      this.unit = this.units[idx]
      this.searchUnit = this.units[idx].uri
      this.units = []
      console.debug('selected unit', this.unit)
    },
    selectConcept (idx) {
      if (idx === null || !this.concepts[idx]) {
        console.warn('select concept does not work', idx, this.concepts[idx])
        console.debug('concepts are', this.concepts)
        return
      }
      this.concept = this.concepts[idx]
      this.searchConcept = this.concepts[idx].uri
      this.concepts = []
      console.debug('selected concept', this.concept)
    },
    async remove (mode) {
      /* update column */
      let payload
      if (mode === 'unit') {
        payload = {
          unit_uri: null,
          concept_uri: (this.column.concept ? this.column.concept.uri : null)
        }
      } else if (mode === 'concept') {
        payload = {
          unit_uri: (this.column.unit ? this.column.unit.uri : null),
          concept_uri: null
        }
      }
      try {
        await this.$axios.put(`/api/container/${this.database.id}/database/${this.database.id}/table/${this.tableId}/column/${this.column.id}`, payload, this.config)
        if (payload.unit_uri === null) {
          this.unit = null
        }
        if (payload.concept_uri === null) {
          this.concept = null
        }
      } catch (error) {
        console.error(`Failed to save column ${mode}`, error)
        const { message } = error.response
        this.$toast.error(`Failed to save column ${mode}: ` + message)
      }
    },
    async retrieveConcepts () {
      this.loadingConcept = true
      try {
        const res = await this.$axios.get(`/api/semantics/concept?q=${this.searchConcept}`, this.config)
        this.concepts = res.data
        console.debug('concepts', this.concepts)
      } catch (error) {
        console.error('Failed to retrieve concepts', error)
        const { message } = error.response
        this.$toast.error('Failed to retrieve concepts: ' + message)
      }
      this.loadingConcept = false
    },
    async retrieveUnits () {
      this.loadingUnit = true
      try {
        const res = await this.$axios.get(`/api/semantics/unit?q=${this.searchUnit}`, this.config)
        this.units = res.data
        console.debug('units', this.units)
      } catch (error) {
        console.error('Failed to retrieve units', error)
        const { message } = error.response
        this.$toast.error('Failed to retrieve units: ' + message)
      }
      this.loadingUnit = false
    },
    async save () {
      this.loadingSave = true
      for (const mode in { unit: 0, concept: 0 }) {
        await this.loadLabelIfNecessary(mode)
        if (this[mode] === null || this[mode].name === null || this[mode].uri === null) {
          console.warn(`Delete ${mode} because object, name or uri is null`)
          await this.remove(mode)
          continue
        }
        try {
          const res = await this.$axios.post(`/api/semantics/${mode}`, {
            name: this[mode].name,
            uri: this[mode].uri
          })
          console.info(`Saved ${mode} with name`, this[mode].name)
          console.debug(`saved ${mode}`, res.data)
        } catch (error) {
          console.error(`Failed to save ${mode}`, error)
          const { status, message } = error.response
          if (status === 409) {
            console.debug(`${mode} already saved, skipping`)
          } else {
            console.error(`Failed to save ${mode}`, error)
            this.$toast.error(`Failed to save ${mode}: ` + message)
          }
        }
      }
      await this.update()
      this.loadingSave = false
    },
    reset () {
      if (this.column.concept) {
        this.searchConcept = this.column.concept.uri
        this.concept = this.column.concept
      } else {
        this.searchConcept = null
        this.concept = null
      }
      if (this.column.unit) {
        this.searchUnit = this.column.unit.uri
        this.unit = this.column.unit
      } else {
        this.searchUnit = null
        this.unit = null
      }
    },
    async update () {
      try {
        const payload = {
          concept_uri: this.concept === null ? null : this.concept.uri,
          unit_uri: this.unit === null ? null : this.unit.uri
        }
        await this.$axios.put(`/api/container/${this.database.id}/database/${this.database.id}/table/${this.tableId}/column/${this.column.id}`, payload, this.config)
        this.$emit('close', {
          success: true,
          action: 'assign',
          concept: (payload.concept_uri === null ? null : this.concept),
          unit: (payload.unit_uri === null ? null : this.unit)
        })
      } catch (error) {
        console.error('Failed to update column', error)
        const { message } = error.response
        this.$toast.error('Failed to update column: ' + message)
      }
    },
    async loadLabelIfNecessary (mode) {
      const uri = (mode === 'unit' ? this.searchUnit : this.searchConcept)
      if (uri === null || uri === '') {
        this[mode] = null
        return
      }
      console.debug('load label for mode', mode)
      if (this[mode] !== null && 'uri' in this[mode] && this[mode].uri !== null && this[mode].uri === uri) {
        return
      }
      try {
        const res = await this.$axios.put(`/api/semantics/${mode}`, { uri }, this.config)
        const { label } = res.data
        this[mode] = {
          uri,
          name: label
        }
        console.debug(`${mode}`, this[mode])
      } catch (error) {
        console.error('Failed to load label', error)
        const { message, status } = error.response
        if (status === 400) {
          console.error(`Failed to retrieve ${mode}, not a valid entity`, error)
          this.$toast.error(`Failed to retrieve ${mode}, not a valid entity: ` + message)
        } else {
          console.error(`Failed to retrieve ${mode}`, error)
          this.$toast.error(`Failed to retrieve ${mode}: ` + message)
        }
        this.$toast.error('Failed to load label: ' + message)
      }
    },
    submit () {
      this.$refs.form.validate()
    }
  }
}
</script>
<style scoped>
</style>
