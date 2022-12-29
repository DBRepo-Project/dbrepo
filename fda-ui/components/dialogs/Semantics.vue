<template>
  <div>
    <v-form v-model="valid">
      <v-card>
        <v-card-title>
          {{ title }}
        </v-card-title>
        <v-card-subtitle>
          Assign a {{ subtitle }} to column <strong>{{ column.name }}</strong>
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
    mode: {
      type: String,
      default: () => 'unit'
    },
    databaseId: {
      type: Number,
      default: () => -1
    },
    tableId: {
      type: Number,
      default: () => -1
    }
  },
  data () {
    return {
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
    title () {
      return this.unit ? 'Unit of measurement' : 'Concept'
    },
    subtitle () {
      return this.unit ? 'unit of measurement' : 'semantic concept'
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
    column (newVal, oldVal) {
      this.loadUri(newVal)
    },
    model (newVal, oldVal) {
      /* selected semantic concept or unit */
      this.column[this.mode] = newVal
      this.loadUri(this.column)
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
    this.loadUri(this.column)
  },
  methods: {
    cancel () {
      this.$emit('close', {
        success: false,
        action: 'cancel'
      })
    },
    async loadUri (column) {
      if (!column) {
        return
      }
      if (!column[this.mode]) {
        console.warn('something wrong mode', this.mode, 'column', column)
        return
      }
      const url = `/api/semantics/${this.mode}/${encodeURI(column[this.mode].name)}`
      console.debug('load uri', url)
      try {
        const res = await this.$axios.get(url)
        this.uri = res.data
        console.debug('concept uri loaded', res.data)
      } catch (err) {
        this.$toast.error('Failed to load uri')
        console.error('Failed to load uri', err)
      }
    },
    remove () {
      /* delete assignment */
    },
    async save () {
      /* save semantics */
      const url = `/api/semantics/${this.mode}`
      try {
        const payload = {
          name: this.model.name,
          uri: this.uri
        }
        console.debug('save', payload, 'url', url)
        const res = await this.$axios.post(url, payload)
        this.column[this.mode].uri = res.data.uri
        console.info('Semantic', this.mode, 'saved', res.data)
      } catch (error) {
        const { status } = error.response
        if (status === 409) {
          console.debug('concept already saved, skipping')
        } else {
          this.$toast.error('Could not save concept')
          console.error('save', error)
        }
      }
      /* update column */
      try {
        const payload = {
          concept_uri: (!this.column.concept ? null : this.column.concept.uri),
          unit_uri: (!this.column.unit ? null : this.column.unit.uri)
        }
        const res = await this.$axios.put(`/api/container/${this.databaseId}/database/${this.databaseId}/table/${this.tableId}/column/${this.column.id}`, payload, this.config)
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
        this.$toast.error('Could not save column unit')
        console.error('save', err)
      }
    }
  }
}
</script>

<style scoped>
</style>
