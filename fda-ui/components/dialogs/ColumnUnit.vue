<template>
  <div>
    <v-card>
      <v-card-title>
        Assign Unit of Measurement
      </v-card-title>
      <v-card-text>
        <v-autocomplete
          v-model="model"
          solo
          clearable
          auto-select-first
          :cache-items="false"
          autofocus
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
          <v-list-item v-if="model.uri" three-line>
            <v-list-item-content>
              <v-list-item-title>URI</v-list-item-title>
              <v-list-item-subtitle>{{ model.uri }}</v-list-item-subtitle>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-expand-transition>
      <v-card-actions>
        <v-spacer />
        <v-btn
          class="mb-2 mr-2"
          @click="cancel">
          Cancel
        </v-btn>
        <v-btn
          color="primary"
          class="mb-2"
          :disabled="!model"
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
    concept: {
      type: Object,
      default: () => ({})
    },
    tableId: { type: Number, default: () => -1 }
  },
  data () {
    return {
      dialog: false,
      isLoading: false,
      saved: false,
      model: {
        name: null,
        uri: null,
        symbol: null
      },
      uri: null,
      search: null,
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
      this.loadConcept(newVal)
    },
    async search (val) {
      if (this.isLoading) { return }
      if (!val || !val.length) { return }
      this.isLoading = true
      try {
        const res = await this.$axios.post('/api/units/suggest', {
          offset: 0,
          ustring: this.search
        })
        this.entries = res.data
      } catch (err) {
        this.$toast.error('Could not load unit suggestions.')
        console.log(err)
      }
      this.isLoading = false
    }
  },
  mounted () {
    this.loadConcept(this.concept)
  },
  methods: {
    cancel () {
      this.$emit('close', {
        success: false
      })
    },
    async loadConcept (concept) {
      if (!concept) {
        return
      }
      this.model = concept
      console.debug('load concept', concept)
      try {
        const res = await this.$axios.get(`/api/units/uri/${concept.name}`)
        this.model.uri = res.data.URI
      } catch (err) {
        this.$toast.error(`Could not load URI of unit "${concept.name}"`)
        console.log(err)
      }
    },
    async save () {
      const payload = {
        name: this.model.name,
        uri: this.model.uri
      }
      try {
        console.debug('save', payload)
        await this.$axios.post('/api/units/saveconcept', payload)
      } catch (error) {
        const { status } = error.response
        if (status !== 201 && status !== 400) {
          this.$toast.error('Could not save concept.')
          console.log(error)
        }
      }
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
        this.$emit('close', {
          success: true,
          concept: res.data
        })
        console.debug('column', this.column)
      } catch (err) {
        this.$toast.error('Could not save column unit.')
        console.log(err)
      }
    }
  }
}
</script>

<style scoped>
</style>
