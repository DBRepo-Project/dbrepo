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
              :error-messages="needsSequence && c.name.toLowerCase() === 'id' ? ['Column needs to be declared as primary key'] : []"
              label="Name *" />
          </v-col>
          <v-col cols="2">
            <v-select
              v-model="c.type"
              :items="columnTypes"
              item-value="value"
              required
              :rules="[v => !!v || $t('Required')]"
              label="Data Type *"
              @change="setDefaultSizeAndD(c)" />
          </v-col>
          <v-col cols="2" :hidden="c.type !== 'set'">
            <v-text-field
              v-model="c.sets_values"
              required
              counter
              :counter-value="() => c.sets.length"
              hint="Separate values by ,"
              :rules="[v => !!v || $t('Required')]"
              label="Set Values *"
              @focusout="formatValues(c)" />
          </v-col>
          <v-col cols="2" :hidden="c.type !== 'enum'">
            <v-text-field
              v-model="c.enums_values"
              required
              counter
              :counter-value="() => c.enums.length"
              hint="Separate values by ,"
              :rules="[v => !!v || $t('Required')]"
              label="Enum Values *"
              @focusout="formatValues(c)" />
          </v-col>
          <v-col cols="1" :hidden="defaultSize(c) === false">
            <v-text-field
              v-model.number="c.size"
              type="number"
              required
              :rules="[v => (v !== null && v !== '') || $t('Required')]"
              :error-messages="sizeErrorMessages(c)"
              label="size *" />
          </v-col>
          <v-col cols="1" :hidden="defaultD(c) === false">
            <v-text-field
              v-model.number="c.d"
              type="number"
              required
              :rules="[v => (v !== null && v !== '') || $t('Required')]"
              :error-messages="dErrorMessages(c)"
              label="d *" />
          </v-col>
          <v-col v-if="hasDate(c)" cols="2">
            <v-select
              v-model="c.dfid"
              required
              :rules="[v => !!v || $t('Required')]"
              :items="filterDateFormats(c)"
              label="fsp *"
              :item-text="item => `${item.example}`"
              item-value="id" />
          </v-col>
          <v-col v-if="shift(c)" :cols="shift(c)" />
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
        <v-btn color="primary" :loading="loading" class="mt-10 mb-1" @click="submit">
          Continue
        </v-btn>
      </div>
    </v-form>
  </div>
</template>

<script>
import QueryMapper from '@/api/query.mapper'

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
      valid: true,
      finished: false,
      tableColumns: [],
      columnTypes: QueryMapper.mySql8DataTypes()
    }
  },
  computed: {
    database () {
      return this.$store.state.database
    },
    needsSequence () {
      return this.columns.filter(c => c.primary_key).length === 0
    },
    dateFormats () {
      if (!this.database || !('container' in this.database) || !('image' in this.database.container) || !('date_formats' in this.database.container.image)) {
        return []
      }
      return this.database.container.image.date_formats
    }
  },
  methods: {
    shift (column) {
      if (!this.columns || this.columns.length === 0) {
        return false
      }
      let shift = 0
      if (this.hasDate(column) === false && this.columns.filter(c => this.hasDate(c) !== false).length > 0 && this.defaultSize(column) === false && this.columns.filter(c => this.defaultSize(c) !== false).length > 0) {
        shift++
      }
      if (this.defaultD(column) === false && this.columns.filter(c => this.defaultD(c) !== false).length > 0) {
        shift++
      }
      if (this.hasEnumOrSet(column) === false && this.columns.filter(c => this.hasEnumOrSet(c) !== false).length > 0) {
        shift++
      }
      return shift
    },
    submit () {
      this.finished = true
      this.loading = true
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
        dfid: null,
        sets: [],
        sets_values: null,
        enums: [],
        enums_values: null,
        size: 0,
        d: 0
      })
    },
    formatValues (column) {
      if (column.type === 'set') {
        if (!column.sets_values || column.sets_values.length === 0) {
          return
        }
        column.sets = column.sets_values.split(',').map(v => v.trim())
      } else if (column.type === 'enum') {
        if (!column.enums_values || column.enums_values.length === 0) {
          return
        }
        column.enums = column.enums_values.split(',').map(v => v.trim())
      }
    },
    defaultSize (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      if (filter[0].defaultSize === undefined || filter[0].defaultSize === null) {
        return false
      }
      return filter[0].defaultSize
    },
    defaultD (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      if (filter[0].defaultD === undefined || filter[0].defaultD === null) {
        return false
      }
      return filter[0].defaultD
    },
    setDefaultSizeAndD (column) {
      column.size = this.defaultSize(column)
      column.d = this.defaultD(column)
    },
    hasDate (column) {
      return column.type === 'date' || column.type === 'datetime' || column.type === 'timestamp' || column.type === 'time'
    },
    hasEnumOrSet (column) {
      return column.type === 'enum' || column.type === 'set'
    },
    filterDateFormats (column) {
      return this.dateFormats.filter((df) => {
        if (column.type === 'date') {
          return !df.has_time
        }
        return df.has_time
      })
    },
    sizeErrorMessages (column) {
      if (column.size < column.d) {
        return ['Size needs to be bigger or equal to d']
      }
      if (column.size < 0) {
        return ['Size must be positive']
      }
      return []
    },
    dErrorMessages (column) {
      if (column.size < column.d) {
        return ['D needs to be smaller or equal to size']
      }
      if (column.d < 0) {
        return ['D must be positive']
      }
      return []
    }
  }
}
</script>

<style scoped>
</style>
