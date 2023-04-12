<template>
  <div>
    <v-alert
      v-if="needsSequence"
      border="left"
      color="info">
      We create a column named <code>id</code> with a auto-increasing sequence starting at 1. Please specify a column with primary key if you don't want this behavior.
    </v-alert>
    <v-form ref="form" v-model="valid">
      <div v-for="(c, idx) in columns" :key="idx">
        <v-row dense class="column pa-2 ml-1 mr-1 mb-2">
          <v-col cols="2">
            <v-text-field
              v-model="c.name"
              required
              :rules="[v => !!v || $t('Required')]"
              :error-messages="needsSequence && c.name === 'id' ? ['Column needs to be declared as primary key'] : []"
              label="Name *" />
          </v-col>
          <v-col cols="2">
            <v-select
              v-model="c.type"
              :items="columnTypes"
              item-value="value"
              required
              :rules="[v => !!v || $t('Required')]"
              label="Data Type *" />
          </v-col>
          <v-col cols="2" :hidden="c.type !== 'ENUM'">
            <v-select
              v-model="c.enum_values"
              :disabled="c.type !== 'ENUM'"
              :items="c.suggestions"
              :menu-props="{ maxHeight: '400' }"
              label="Enumeration"
              multiple />
          </v-col>
          <v-col v-if="c.type.match('(timestamp)|(date)')" cols="2">
            <v-select
              v-if="c.type !== 'timestamp'"
              v-model="c.dfid"
              required
              :rules="[v => !!v || $t('Required')]"
              :items="dateFormats.filter(f => !f.has_time)"
              label="Date Format *"
              :item-text="item => `${item.example}`"
              item-value="id" />
            <v-select
              v-if="c.type !== 'date'"
              v-model="c.dfid"
              required
              :rules="[v => !!v || $t('Required')]"
              :items="dateFormats.filter(f => f.has_time)"
              label="Timestamp Format *"
              :item-text="item => `${item.example}`"
              item-value="id" />
          </v-col>
          <v-col v-if="needsShift(c)" cols="2" />
          <v-col cols="auto" class="pl-10" :hidden="c.type !== 'string' || c.type !== 'VARCHAR'">
            <v-text-field v-model="c.check_expression" label="Check Expression" />
          </v-col>
          <v-col cols="auto" class="pl-2">
            <v-checkbox v-model="c.primary_key" label="Primary Key" @click="setOthers(c)" />
          </v-col>
          <v-col cols="auto" class="pl-10">
            <v-checkbox v-model="c.null_allowed" :disabled="c.primary_key" label="Null" />
          </v-col>
          <v-col cols="auto" class="pl-10">
            <v-checkbox v-model="c.unique" :hidden="c.primary_key" label="Unique" />
          </v-col>
          <v-col v-if="false" cols="auto" class="pl-10">
            <v-text-field v-model="c.foreign_key" hidden required label="Foreign Key" />
          </v-col>
          <v-col v-if="false" cols="auto" class="pl-10">
            <v-text-field v-model="c.references" hidden required label="References" />
          </v-col>
          <v-col v-if="canRemove(idx)" cols="auto" class="mt-5 ml-5">
            <v-btn x-small @click="removeColumn(idx)">
              Remove
            </v-btn>
          </v-col>
        </v-row>
      </div>
      <div>
        <v-btn x-small @click="addColumn()">
          Add Column
        </v-btn>
      </div>
      <div>
        <v-btn v-if="back" class="mt-10 mr-2 mb-1" @click="stepBack()">
          Back
        </v-btn>
        <v-btn color="primary" :loading="localLoading" :disabled="!valid" class="mt-10 mb-1" @click="submit">
          Continue
        </v-btn>
      </div>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    columns: {
      type: Array,
      default () {
        return []
      }
    },
    back: {
      type: Boolean,
      default () {
        return false
      }
    },
    error: {
      type: Boolean,
      default () {
        return false
      }
    },
    loading: {
      type: Boolean,
      default () {
        return false
      }
    }
  },
  data () {
    return {
      localLoading: false,
      dateFormats: [],
      valid: true,
      finished: false,
      tableColumns: [],
      container: {
        image: {
          id: null
        }
      },
      columnTypes: [
        // { value: 'ENUM', text: 'Enumeration' }, // Disabled for now, not implemented, #145
        { value: 'boolean', text: 'Boolean' },
        { value: 'number', text: 'Number' },
        { value: 'blob', text: 'Binary Large Object' },
        { value: 'date', text: 'Date' },
        { value: 'decimal', text: 'Floating Number' },
        { value: 'timestamp', text: 'Timestamp' },
        { value: 'decimal', text: 'Decimal' },
        { value: 'string', text: 'Character Varying' },
        { value: 'text', text: 'Text' }
      ]
    }
  },
  computed: {
    needsSequence () {
      return this.columns.filter(c => c.primary_key).length === 0
    }
  },
  watch: {
    loading () {
      this.localLoading = this.loading
    }
  },
  mounted () {
    this.localLoading = this.loading
    this.loadContainer()
      .then(() => this.loadImage())
  },
  methods: {
    needsShift (column) {
      if (column.type === 'date' || column.type === 'timestamp') {
        return false
      }
      return this.columns.filter(c => c.type === 'date' || c.type === 'timestamp').length > 0
    },
    async loadContainer () {
      const getUrl = `/api/container/${this.$route.params.container_id}`
      try {
        this.localLoading = true
        const res = await this.$axios.get(getUrl)
        this.container = res.data
        console.debug('retrieve container', this.container)
      } catch (err) {
        this.error = true
        console.error('retrieve image date formats failed', err)
      }
      this.localLoading = false
    },
    async loadImage () {
      const getUrl = `/api/image/${this.container.image.id}`
      try {
        this.localLoading = true
        const res = await this.$axios.get(getUrl)
        this.dateFormats = res.data.date_formats
        console.debug('retrieve image date formats', this.dateFormats)
      } catch (err) {
        this.error = true
        console.error('retrieve image date formats failed', err)
      }
      this.localLoading = false
    },
    submit () {
      this.finished = true
      this.localLoading = true
      this.$emit('close', { success: true })
    },
    setOthers (column) {
      column.null_allowed = false
      column.unique = true
    },
    stepBack () {
      this.$emit('close', { success: false })
    },
    canRemove (idx) {
      if (idx > 0) {
        return true
      }
      if (this.needsSequence) {
        return true
      }
      if (this.columns[0].primary_key) {
        return false
      }
      return false
    },
    removeColumn (idx) {
      this.columns.splice(idx, 1)
    },
    addColumn (name = '', type = '', null_allowed = true, primary_key = false, unique = false) {
      this.columns.push({
        name,
        type,
        null_allowed,
        primary_key
      })
    }
  }
}
</script>

<style scoped>
</style>
