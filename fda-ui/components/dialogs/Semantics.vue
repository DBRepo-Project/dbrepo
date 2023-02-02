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
              single-line
              hide-details
              placeholder="Search or provide URI..." />
            <v-btn icon small class="ml-2" type="submit" @click="retrieveConcept">
              <v-icon>mdi-magnify</v-icon>
            </v-btn>
          </v-toolbar>
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
              single-line
              hide-details
              placeholder="Search or provide URI..." />
            <v-btn icon small class="ml-2" type="submit" @click="retrieveUnit">
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
        <v-btn
          v-if="canRemove"
          class="mb-2 ml-2"
          color="error"
          @click="remove">
          Remove
        </v-btn>
        <v-spacer />
        <v-btn
          class="mb-2"
          @click="cancel">
          Cancel
        </v-btn>
        <v-btn
          color="primary"
          class="mb-2 mr-2"
          :disabled="!canSave"
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
    canRemove () {
      return this.column[this.mode] !== null
    },
    canSave () {
      return ('uri' in this.unit || 'uri' in this.concept)
    }
  },
  watch: {
    async search (val) {
      if (!val || this.selected) {
        return
      }
      this.searchTerm = val
      this.isLoading = true
      await new Promise(resolve => setTimeout(resolve, 2000))
      if (val !== this.searchTerm) {
        return
      }
      try {
        const res = await this.$axios.get(`/api/semantics/${this.mode}?q=${val}`, this.config)
        this.entries = res.data
        console.debug('suggest', res.data)
      } catch (err) {
        console.error('suggest', err)
      }
      this.isLoading = false
    }
  },
  mounted () {
  },
  methods: {
    cancel () {
      this.$emit('close', {
        success: false,
        action: 'cancel'
      })
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
    async remove () {
      if (!this.database) {
        return
      }
      /* update column */
      let payload
      if (this.mode === 'unit') {
        payload = {
          unit_uri: null,
          concept_uri: (this.column.concept ? this.column.concept.uri : null)
        }
      } else if (this.mode === 'concept') {
        payload = {
          unit_uri: (this.column.unit ? this.column.unit.uri : null),
          concept_uri: null
        }
      }
      try {
        await this.$axios.put(`/api/container/${this.database.id}/database/${this.database.id}/table/${this.tableId}/column/${this.column.id}`, payload, this.config)
        if (payload.unit_uri === null) {
          this.column[this.mode] = null
        }
        if (payload.concept_uri === null) {
          this.column[this.mode] = null
        }
        this.dialog = false
        this.saved = true
        console.info(`Removed semantics of column ${this.column.name}`)
        this.$toast.success(`Removed semantics of column ${this.column.name}`)
        this.$emit('close', {
          success: true,
          action: 'remove',
          mode: this.mode
        })
        console.debug('column', this.column)
      } catch (error) {
        console.error('Failed to save column semantics', error)
        const { message } = error.response
        this.$toast.error('Failed to save column semantics: ' + message)
      }
    },
    resetModel (name, uri) {
      this.selected = false
      this.model = {
        name,
        uri,
        symbol: null,
        comment: null
      }
    },
    async retrieveConcept () {
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
    async retrieveUnit () {
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
      for (const mode in { unit: 0, concept: 0 }) {
        if (this[mode].name == null || this[mode].uri == null) {
          return
        }
        try {
          const res = await this.$axios.post(`/api/semantics/${mode}`, {
            name: this[mode].name,
            uri: this[mode].uri
          })
          this.column[mode] = this[mode]
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
        await this.update(mode)
      }
    },
    async update (mode) {
      try {
        const payload = {
          concept_uri: this.concept.uri,
          unit_uri: this.unit.uri
        }
        const res = await this.$axios.put(`/api/container/${this.database.id}/database/${this.database.id}/table/${this.tableId}/column/${this.column.id}`, payload, this.config)
        if (mode === 'unit') {
          if (res.data.unit_uri === null) {
            this.column[mode] = null
          } else {
            this.column[mode] = {
              name: this[mode].name,
              uri: res.data.unit_uri
            }
          }
        }
        if (mode === 'concept') {
          if (res.data.concept_uri === null) {
            this.column[mode] = null
          } else {
            this.column[mode] = {
              name: this[mode].name,
              uri: res.data.concept_uri
            }
          }
        }
        this.dialog = false
        this.saved = true
        console.info(`Updated semantics of column ${this.column.name}`)
        this.$toast.success(`Updated semantics of column ${this.column.name}`)
        this.$emit('close', {
          success: true,
          action: 'assign',
          mode,
          data: res.data
        })
        console.debug('column', this.column)
      } catch (error) {
        console.error('Failed to update column', error)
        const { message } = error.response
        this.$toast.error('Failed to update column: ' + message)
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
