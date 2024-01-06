<template>
  <div v-if="localTuple">
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title v-text="title" />
        <v-card-subtitle v-if="subtitle" v-text="subtitle" />
        <v-card-text>
          <div v-for="(column,idx) in table.columns" :key="idx">
            <v-text-field
              v-if="isNumber(column)"
              v-model.number="localTuple[column.internal_name]"
              :disabled="(!edit && column.auto_generated)"
              class="mb-2"
              :hint="hint(column)"
              persistent-hint
              :rules="rules(column)"
              :required="required(column)"
              :label="label(column)"
              type="number" />
            <v-text-field
              v-if="isTextField(column)"
              v-model="localTuple[column.internal_name]"
              :disabled="disabled(column)"
              class="mb-2"
              :clearable="!required(column)"
              :counter="maxLength(column) !== null"
              :maxlength="maxLength(column)"
              :rules="rules(column)"
              :required="required(column)"
              :label="label(column)"
              type="text" />
            <v-text-field
              v-if="isFloatingPoint(column)"
              v-model="localTuple[column.internal_name]"
              :disabled="disabled(column)"
              class="mb-2"
              step=".1"
              :clearable="!required(column)"
              :rules="rules(column)"
              :required="required(column)"
              :hint="hint(column)"
              :label="label(column)"
              type="number" />
            <v-file-input
              v-if="isFileField(column)"
              v-model="localTuple[column.internal_name]"
              :disabled="disabled(column)"
              prepend-icon="mdi-code-brackets"
              class="mb-2"
              :clearable="!required(column)"
              :rules="rules(column)"
              :required="required(column)"
              :hint="hint(column)"
              :show-size="1000"
              counter
              :label="label(column)"
              type="file"
              @focusout="upload(column, localTuple[column.internal_name])" />
            <v-textarea
              v-if="isTextArea(column)"
              v-model="localTuple[column.internal_name]"
              :disabled="disabled(column)"
              class="mb-2"
              rows="3"
              :clearable="!required(column)"
              :rules="rules(column)"
              :required="required(column)"
              :hint="hint(column)"
              :label="label(column)" />
            <v-text-field
              v-if="isTimeField(column)"
              v-model="localTuple[column.internal_name]"
              :hint="hint(column)"
              persistent-hint
              class="mb-2"
              :clearable="!required(column)"
              :required="required(column)"
              :label="label(column)"
              type="text" />
            <v-select
              v-if="isSet(column) || isEnum(column)"
              v-model="localTuple[column.internal_name]"
              class="mb-2"
              :rules="rules(column)"
              :required="required(column)"
              :clearable="!required(column)"
              :items="isSet(column) ? column.sets : column.enums"
              :label="label(column)" />
            <v-select
              v-if="isBoolean(column)"
              v-model="localTuple[column.internal_name]"
              class="mb-2"
              :rules="rules(column)"
              :required="required(column)"
              :items="bools"
              :clearable="!required(column)"
              :label="label(column)" />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            v-if="!edit"
            id="addTuple"
            class="mb-2"
            :disabled="!valid"
            color="primary"
            type="submit"
            @click="addTuple">
            Create
          </v-btn>
          <v-btn
            v-if="edit"
            id="updateTuple"
            class="mb-2 ml-3 mr-2"
            :disabled="!valid"
            color="primary"
            type="submit"
            @click="updateTuple">
            Update
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import QueryService from '@/api/query.service'
import UploadService from '@/api/upload.service'

export default {
  props: {
    tuple: {
      type: Object,
      default: null
    },
    edit: {
      type: Boolean,
      default: false
    },
    table: {
      type: Object,
      default: () => {
        return {
          columns: [],
          constraints: {
            checks: []
          }
        }
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      menu: false,
      localTuple: null,
      localDisplay: null,
      bools: [
        { text: 'true', value: true },
        { text: 'false', value: false }
      ]
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    title () {
      return (this.edit ? 'Edit' : 'Add') + ' Tuple'
    },
    subtitle () {
      if (!this.table.constraints) {
        return null
      }
      return this.table.constraints.checks.length > 0 ? `Constraints: ${this.table.constraints.checks}` : null
    }
  },
  watch: {
    tuple (val) {
      this.localTuple = Object.assign({}, val)
      this.localDisplay = Object.assign({}, val)
    }
  },
  mounted () {
    this.localTuple = Object.assign({}, this.tuple)
    this.localDisplay = Object.assign({}, this.tuple)
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.menu = false
      this.$emit('close', { success: false })
    },
    hint (column) {
      if (!this.edit && column.auto_generated) {
        return 'Auto-generated by sequence'
      }
      if (this.edit && column.is_primary_key) {
        return 'Required (Primary Key)'
      }
      if (['double', 'decimal'].includes(column.column_type)) {
        return `Floating point number max. ${column.size} digit${column.size !== 1 ? 's' : ''} before and max. ${column.d} digit${column.d !== 1 ? 's' : ''} after the dot`
      }
      if (['date', 'datetime', 'timestamp', 'time'].includes(column.column_type)) {
        return `Format: ${column.date_format.unix_format}`
      }
      if (['year'].includes(column.column_type)) {
        return 'Format: YYYY'
      }
    },
    label (column) {
      return column.name + (!column.is_null_allowed ? ' *' : '')
    },
    isTextField (column) {
      return ['char', 'varchar', 'tinytext', 'mediumtext'].includes(column.column_type)
    },
    isTextArea (column) {
      return ['text'].includes(column.column_type)
    },
    isFileField (column) {
      return ['blob', 'longblob', 'mediumblob', 'tinyblob'].includes(column.column_type)
    },
    isBoolean (column) {
      return ['bool'].includes(column.column_type)
    },
    isNumber (column) {
      return ['int', 'binary', 'bit', 'tinyint', 'smallint', 'mediumint', 'bigint'].includes(column.column_type)
    },
    isFloatingPoint (column) {
      return ['float', 'double', 'decimal'].includes(column.column_type)
    },
    isEnum (column) {
      return column.column_type === 'enum'
    },
    isSet (column) {
      return column.column_type === 'set'
    },
    isTimeField (column) {
      return ['date', 'datetime', 'timestamp', 'time', 'year'].includes(column.column_type)
    },
    rules (column) {
      if (column.auto_generated || column.is_null_allowed) {
        return []
      }
      const rules = []
      rules.push(v => !!v || 'Required')
      if (column.column_type === 'char') {
        rules.push(v => !(!v || v.length !== column.size) || `Must be exactly ${column.size} character${column.size !== 1 ? 's' : ''}`)
      }
      if (column.column_type === 'decimal' || column.column_type === 'double') {
        rules.push(v => !(!v || v.split('.')[0].length > column.size) || `max. ${column.size} digit${column.size !== 1 ? 's' : ''} before the dot`)
        rules.push(v => !(!v || v.split('.')[1] > column.d) || `max. ${column.d} digit${column.d !== 1 ? 's' : ''} after the dot`)
      }
      return rules
    },
    maxLength (column) {
      if (!this.isTextField(column) || column.size === null) {
        return null
      }
      return column.size
    },
    required (column) {
      return column.is_null_allowed === false
    },
    disabled (column) {
      return (this.edit && column.is_primary_key) || (!this.edit && column.auto_generated)
    },
    updateTuple () {
      const constraints = {}
      this.table.columns
        .filter(c => c.is_primary_key)
        .forEach((c) => {
          constraints[c.internal_name] = this.tuple[c.internal_name]
        })
      const data = {
        data: this.localTuple,
        keys: constraints
      }
      QueryService.updateTuple(this.$route.params.database_id, this.$route.params.table_id, data)
        .then(() => {
          this.$toast.success('Successfully updated tuple!')
          this.$emit('close', { success: true })
        })
    },
    addTuple () {
      const constraints = {}
      this.table.columns
        .filter(c => c.is_primary_key)
        .forEach((c) => {
          constraints[c.internal_name] = this.localTuple[c.internal_name]
        })
      this.table.columns.forEach((column) => {
        if (!(column.internal_name in this.localTuple)) {
          this.localTuple[column.internal_name] = null
          this.localDisplay[column.internal_name] = null
        }
      })
      QueryService.insertTuple(this.$route.params.database_id, this.$route.params.table_id, this.localTuple)
        .then(() => {
          this.$toast.success('Successfully added tuple!')
          this.$emit('close', { success: true })
        })
    },
    upload (column, file) {
      if (!file) {
        return
      }
      UploadService.upload(file)
        .then((metadata) => {
          console.debug('uploaded file', metadata)
          const { s3key } = metadata
          this.localDisplay[column.internal_name] = this.localTuple[column.internal_name]
          this.localTuple[column.internal_name] = s3key
        })
        .catch((error) => {
          console.error(`Failed to set column value: ${column.internal_name}`, error)
          this.$toast.error(`Failed to set column value: ${column.internal_name}`)
        })
    }
  }
}
</script>
