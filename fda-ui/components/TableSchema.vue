<template>
  <div>
    <v-alert
      v-if="needsSequence"
      border="left"
      color="amber lighten-4 black--text">
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
              :error-messages="needsSequence && c.name === 'id' ? ['Column with this name already present'] : []"
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
          <v-col v-if="c.type.match('(TIMESTAMP)|(DATE)')" cols="2" class="pl-10">
            <v-select
              v-if="c.type !== 'TIMESTAMP'"
              v-model="c.dfid"
              required
              :rules="[v => !!v || $t('Required')]"
              :items="dateFormats.filter(f => !f.has_time)"
              label="Date Format *"
              item-text="example"
              item-value="id" />
            <v-select
              v-if="c.type !== 'DATE'"
              v-model="c.dfid"
              required
              :rules="[v => !!v || $t('Required')]"
              :items="dateFormats.filter(f => f.has_time)"
              label="Timestamp Format *"
              item-text="example"
              item-value="id" />
          </v-col>
          <v-col cols="auto" class="pl-10" :hidden="c.type !== 'STRING' || c.type !== 'VARCHAR'">
            <v-text-field v-model="c.check_expression" label="Check Expression" />
          </v-col>
          <v-col cols="auto" class="pl-2">
            <v-checkbox v-model="c.primary_key" label="Primary Key" @click="setOthers(c)" />
          </v-col>
          <v-col cols="auto" class="pl-10">
            <v-checkbox v-model="c.null_allowed" :disabled="c.primary_key" label="Null Allowed" />
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
              Remove Column
            </v-btn>
          </v-col>
        </v-row>
      </div>
      <div>
        <v-btn x-small :loading="loading" @click="addColumn()">
          Add Column
        </v-btn>
      </div>
      <div>
        <v-btn v-if="back" class="mt-10 mr-2 mb-1" @click="stepBack()">
          Back
        </v-btn>
        <v-btn color="primary" :loading="finished" :disabled="!valid" class="mt-10 mb-1" @click="submit()">
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
    }
  },
  data () {
    return {
      loading: false,
      dateFormats: [],
      valid: true,
      finished: false,
      tableColumns: [],
      columnTypes: [
        // { value: 'ENUM', text: 'Enumeration' }, // Disabled for now, not implemented, #145
        { value: 'BOOLEAN', text: 'Boolean' },
        { value: 'NUMBER', text: 'Number' },
        { value: 'BLOB', text: 'Binary Large Object' },
        { value: 'DATE', text: 'Date' },
        { value: 'DECIMAL', text: 'Floating Number' },
        { value: 'TIMESTAMP', text: 'Timestamp' },
        { value: 'DECIMAL', text: 'Decimal' },
        { value: 'STRING', text: 'Character Varying' },
        { value: 'TEXT', text: 'Text' }
      ]
    }
  },
  computed: {
    needsSequence () {
      return this.columns.filter(c => c.primary_key).length === 0
    }
  },
  mounted () {
    this.loadDateFormats()
  },
  methods: {
    async loadDateFormats () {
      const getUrl = `/api/container/${this.$route.params.container_id}`
      let getResult
      try {
        this.loading = true
        getResult = await this.$axios.get(getUrl)
        this.dateFormats = getResult.data.image.date_formats
        console.debug('retrieve image date formats', this.dateFormats)
        this.loading = false
      } catch (err) {
        this.loading = false
        console.error('retrieve image date formats failed', err)
      }
    },
    submit () {
      this.finished = true
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
        primary_key,
        check_expression: null,
        foreign_key: null,
        references: null,
        unique
      })
    }
  }
}
</script>

<style scoped>
</style>
