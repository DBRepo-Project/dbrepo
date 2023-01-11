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
            :placeholder="`Search ${title}s`"
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
        <v-list v-if="model" dense>
          <v-list-item v-if="model.name">
            <v-list-item-content>
              <v-list-item-title>{{ title }} Name</v-list-item-title>
              <v-list-item-content>{{ model.name }}</v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="model.symbol">
            <v-list-item-content>
              <v-list-item-title>{{ title }} Symbol</v-list-item-title>
              <v-list-item-content>{{ model.symbol }}</v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="model.comment">
            <v-list-item-content>
              <v-list-item-title>{{ title }} Comment</v-list-item-title>
              <v-list-item-content>{{ model.comment }}</v-list-item-content>
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-if="model.uri" three-line>
            <v-list-item-content>
              <v-list-item-title>{{ title }} URI</v-list-item-title>
              <v-list-item-content>
                <a :href="model.uri" target="_blank">{{ model.uri }}</a>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
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
        uri: null,
        name: null,
        symbol: null,
        comment: null
      },
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
    subtitle () {
      if (this.mode === 'unit') {
        return 'unit of measurement'
      }
      if (this.mode === 'concept') {
        return 'semantic concept'
      }
      return null
    },
    title () {
      if (this.mode === 'unit') {
        return 'Unit of measurement'
      }
      if (this.mode === 'concept') {
        return 'Concept'
      }
      return null
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
      return this.model
    }
  },
  watch: {
    column (newVal, oldVal) {
      if (newVal[this.mode]) {
        this.resetModel(newVal[this.mode].name, newVal[this.mode].uri)
      } else {
        this.resetModel(null, null)
      }
    },
    mode (newVal, oldVal) {
      if (this.column[this.mode]) {
        this.resetModel(this.column[this.mode].name, this.column[this.mode].uri)
      } else {
        this.resetModel(null, null)
      }
    },
    model (newVal, oldVal) {
      /* selected semantic concept or unit */
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
    if (this.column[this.mode]) {
      this.resetModel(this.column[this.mode].name, this.column[this.mode].uri)
    }
  },
  methods: {
    cancel () {
      this.$emit('close', {
        success: false,
        action: 'cancel'
      })
    },
    remove () {
      /* delete assignment */
      this.update()
    },
    resetModel (name, uri) {
      this.model = {
        name,
        uri,
        symbol: null,
        comment: null
      }
    },
    async save () {
      /* save semantics */
      const url = `/api/semantics/${this.mode}`
      try {
        const payload = {
          name: this.model.name,
          uri: this.model.uri
        }
        console.debug('save', payload, 'url', url)
        const res = await this.$axios.post(url, payload)
        this.column[this.mode] = this.model
        console.info('Saved concept/measurement')
        console.debug('saved concept/measurement', res.data)
      } catch (error) {
        console.error('Failed to save', error)
        const { status } = error.response
        if (status === 409) {
          console.debug('concept already saved, skipping')
        } else {
          this.$toast.error('Could not save concept')
          console.error('save', error)
        }
      }
      await this.update()
    },
    async update () {
      /* update column */
      try {
        const payload = {
          concept_uri: (!this.column.concept ? null : this.column.concept.uri),
          unit_uri: (!this.column.unit ? null : this.column.unit.uri)
        }
        const res = await this.$axios.put(`/api/container/${this.databaseId}/database/${this.databaseId}/table/${this.tableId}/column/${this.column.id}`, payload, this.config)
        if (this.mode === 'unit') {
          if (res.data.unit_uri === null) {
            this.column[this.mode] = null
          } else {
            this.column[this.mode] = {
              name: this.model.name,
              uri: res.data.unit_uri
            }
          }
        }
        if (this.mode === 'concept') {
          if (res.data.concept_uri === null) {
            this.column[this.mode] = null
          } else {
            this.column[this.mode] = {
              name: this.model.name,
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
