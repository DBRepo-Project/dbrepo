<template>
  <div>
    <v-form v-model="valid">
      <v-card>
        <v-card-title>
          Unit of Measurement
        </v-card-title>
        <v-card-subtitle>
          Assign a unit of measurement to column <strong>{{ column.name }}</strong>
        </v-card-subtitle>
        <v-card-text>
          <v-autocomplete
            v-model="model"
            solo
            clearable
            auto-select-first
            :cache-items="false"
            autofocus
            :loading="isLoading"
            placeholder="Search Unit of Measurements"
            :search-input.sync="search"
            :items="items"
            hide-no-data
            hide-details
            dense>
            <template
              v-slot:item="{ item, attrs, on }">
              <v-list-item v-bind="attrs" v-on="on">
                <v-list-item-content>
                  <v-list-item-title>{{ item.value.name }}</v-list-item-title>
                  <v-list-item-subtitle>{{ item.value.comment }}</v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </template>
          </v-autocomplete>
        </v-card-text>
        <v-expand-transition>
          <v-list v-if="model" class="lighten-3" subheader three-line>
            <v-list-item v-if="model.name">
              <v-list-item-content>
                <v-list-item-title>Name</v-list-item-title>
                <v-list-item-subtitle>{{ model.name }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="model.symbol">
              <v-list-item-content>
                <v-list-item-title>Symbol</v-list-item-title>
                <v-list-item-subtitle>{{ model.symbol }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="model.comment">
              <v-list-item-content>
                <v-list-item-title>Comment</v-list-item-title>
                <v-list-item-subtitle>{{ model.comment }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="uri" three-line>
              <v-list-item-content>
                <v-list-item-title>URI</v-list-item-title>
                <v-list-item-subtitle>{{ uri }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
          </v-list>
        </v-expand-transition>
        <v-card-actions>
          <v-btn
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
            :disabled="!model || !uri"
            @click="save">
            Save
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    column: {
      type: Object,
      default: () => ({})
    },
    databaseId: { type: Number, default: () => -1 },
    tableId: { type: Number, default: () => -1 }
  },
  data () {
    return {
      cid: null,
      dbid: null,
      dialog: false,
      isLoading: false,
      saved: false,
      valid: false,
      model: {
        name: null,
        symbol: null,
        comment: null
      },
      uri: null,
      search: null,
      searchTerm: null,
      entries: []
    }
  },
  computed: {
    name () {
      return this.saved && this.model && this.model.name
    },
    items () {
      return this.entries && this.entries.map((entry) => {
        return {
          // text: `${entry.name} [${entry.symbol}], ${entry.comment}`,
          text: entry.name,
          value: entry
        }
      })
    }
  },
  watch: {
    concept (newVal, oldVal) {
      this.loadConcept({ column_concept: newVal })
    },
    model (newVal, oldVal) {
      console.debug('selected concept', newVal)
      this.loadConcept({ column_concept: newVal })
    },
    async search (val) {
      if (!val) {
        return
      }
      this.searchTerm = val
      this.isLoading = true
      await new Promise(resolve => setTimeout(resolve, 1000))
      if (val !== this.searchTerm) {
        return
      }
      try {
        const res = await this.$axios.post('/api/units/suggest', {
          offset: 0,
          ustring: val
        })
        this.entries = res.data
        console.debug('suggest', res.data)
      } catch (err) {
        console.error('suggest', err)
      }
      this.isLoading = false
    }
  },
  mounted () {
    this.loadConcept(this.column)
  },
  methods: {
    cancel () {
      this.$emit('close', {
        success: false,
        action: 'cancel'
      })
    },
    async loadConcept (column) {
      if (!column.column_concept) {
        console.warn('column concept is null')
        return
      }
      if (!column.column_concept.name) {
        console.warn('column concept name is null')
        return
      }
      this.cid = column.id
      this.dbid = column.id
      try {
        const res = await this.$axios.get(`/api/units/uri/${column.column_concept.name}`)
        this.uri = res.data.uri
        console.debug('concept uri loaded', this.uri)
      } catch (err) {
        this.$toast.error('Failed to concept')
        console.error('Failed to concept', err)
      }
    },
    async remove () {
      /* delete assignment */
      const payload = {
        cid: this.column.id,
        tid: this.tableId,
        cdbid: this.databaseId
      }
      try {
        await this.$axios.post('/api/units/deletecolumnsconcept', payload)
        this.$toast.success('Deleted concept assignment')
        console.info('Deleted concept assignment')
      } catch (error) {
        this.$toast.error('Could not delete')
        console.error('Failed to delete', error)
      }
      this.$emit('close', {
        success: true,
        action: 'remove',
        data: payload
      })
    },
    async save () {
      const payload = {
        name: this.model.name,
        uri: this.uri
      }
      /* save concept */
      try {
        console.debug('save', payload)
        const res = await this.$axios.post('/api/units/saveconcept', payload)
        console.info('Concept saved')
        console.debug('concept saved', res.data)
      } catch (error) {
        const { status } = error.response
        if (status === 409) {
          console.debug('concept already saved, skipping.')
        } else {
          this.$toast.error('Could not save concept.')
          console.error('save', error)
        }
      }
      /* save concept */
      try {
        const res = await this.$axios.post('/api/units/savecolumnsconcept', {
          cdbid: Number(this.$route.params.database_id),
          cid: this.column.id,
          tid: this.tableId,
          uri: this.uri
        })
        this.column.column_concept = res.data
        this.column.column_concept.name = this.model.name
        this.dialog = false
        this.saved = true
        this.$toast.success(`Assigned unit ${this.model.name}`)
        this.$emit('close', {
          success: true,
          action: 'assign',
          concept: res.data
        })
        console.debug('column', this.column)
      } catch (err) {
        this.$toast.error('Could not save column unit.')
        console.error('save', err)
      }
    }
  }
}
</script>

<style scoped>
</style>
