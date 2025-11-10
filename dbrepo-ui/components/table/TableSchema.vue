<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      :disabled="disabled">
      <v-row
        v-if="showPrimaryKeyWarning">
        <v-col
          lg="8">
          <v-alert
            border="start"
            color="warning">
            {{ $t('pages.table.subpages.import.schema.primary.warn') }}
          </v-alert>
        </v-col>
      </v-row>
      <v-row
        v-for="(c, idx) in columns"
        :key="`r-${idx}`"
        class="column pa-2 ml-1 mr-1 mb-2"
        dense>
        <v-col
          cols="2">
          <v-text-field
            v-model="c.name"
            required
            :rules="[
              v => !!v || $t('validation.required'),
              v => this.columns.filter(column => column.name === v).length === 1 || $t('validation.column.exists')
            ]"
            persistent-hint
            :variant="inputVariant"
            :label="$t('pages.table.subpages.schema.name.label')"
            :hint="$t('pages.table.subpages.schema.name.hint')" />
        </v-col>
        <v-col
          cols="1">
          <v-select
            v-model="c.type"
            :items="columnTypes"
            item-title="display_name"
            item-value="value"
            required
            :rules="[v => !!v || $t('validation.required')]"
            persistent-hint
            :variant="inputVariant"
            :label="$t('pages.table.subpages.schema.type.label')"
            :hint="$t('pages.table.subpages.schema.type.hint')"
            @update:modelValue="setDefaultSizeAndD(c)" />
        </v-col>
        <v-col
          v-if="c.type === 'set'"
          cols="2">
          <v-text-field
            v-model="c.sets_values"
            required
            counter
            persistent-hint
            :variant="inputVariant"
            :counter-value="() => c.sets.length"
            :hint="$t('pages.table.subpages.schema.set.hint')"
            :rules="[
              v => !!v || $t('validation.required')
            ]"
            :label="$t('pages.table.subpages.schema.set.label')"
            @focusout="formatValues(c)" />
        </v-col>
        <v-col
          v-if="c.type === 'enum'"
          cols="2">
          <v-text-field
            v-model="c.enums_values"
            required
            counter
            persistent-hint
            :variant="inputVariant"
            :counter-value="() => c.enums.length"
            :hint="$t('pages.table.subpages.schema.enum.hint')"
            :rules="[
              v => !!v || $t('validation.required')
            ]"
            :label="$t('pages.table.subpages.schema.enum.label')"
            @focusout="formatValues(c)" />
        </v-col>
        <v-col
          cols="1">
          <v-text-field
            v-if="columnType(c) && columnType(c).size_required !== null"
            v-model.number="c.size"
            type="number"
            :min="columnType(c).size_min"
            :max="columnType(c).size_max"
            :step="columnType(c).size_step"
            :hint="sizeHint(c)"
            :clearable="!columnType(c).size_required"
            persistent-hint
            :variant="inputVariant"
            :rules="[
              v => !(columnType(c).size_required && (v === null || v === '')) || $t('validation.required'),
              v => !(columnType(c).size_min ? Number(v) < columnType(c).size_min : false) || $t('validation.min'),
              v => !(columnType(c).size_max ? Number(v) > columnType(c).size_max : false) || $t('validation.max')
            ]"
            :error-messages="sizeErrorMessages(c)"
            :label="$t('pages.table.subpages.schema.size.label')" />
        </v-col>
        <v-col
          cols="1">
          <v-text-field
            v-if="columnType(c) && columnType(c).d_required !== null"
            v-model.number="c.d"
            type="number"
            :min="columnType(c).d_min !== null ? columnType(c).d_min : null"
            :max="columnType(c).d_max !== null ? columnType(c).d_max : null"
            :step="columnType(c).d_step"
            :hint="dHint(c)"
            :clearable="!columnType(c).d_required"
            persistent-hint
            :variant="inputVariant"
            :rules="[
              v => !(columnType(c).d_required && (v === null || v === '')) || $t('validation.required'),
              v => !(columnType(c).d_min ? Number(v) < columnType(c).d_min : false) || $t('validation.min'),
              v => !(columnType(c).d_max ? Number(v) > columnType(c).d_max : false) || $t('validation.max')
            ]"
            :error-messages="dErrorMessages(c)"
            :label="$t('pages.table.subpages.schema.d.label')" />
        </v-col>
        <v-col
          cols="1"
          class="pl-1">
          <v-text-field
            v-model="c.comment"
            required
            persistent-hint
            :variant="inputVariant"
            :label="$t('pages.table.subpages.schema.comment.label')"
            @focusout="formatValues(c)" />
        </v-col>
        <v-col
          cols="auto"
          class="pl-2">
          <v-checkbox
            v-model="c.primary_key"
            :disabled="disabled"
            :label="$t('pages.table.subpages.schema.primary_key.label')"
            @click="setOthers(c)" />
        </v-col>
        <v-col
          cols="auto"
          class="pl-1">
          <v-checkbox
            v-model="c.null_allowed"
            :disabled="c.primary_key || c.type === 'serial' || disabled"
            :label="$t('pages.table.subpages.schema.null.label')" />
        </v-col>
        <v-col
          cols="auto"
          class="pl-1">
          <v-checkbox
            v-model="c.unique"
            :disabled="disabled || c.type === 'serial'"
            :label="$t('pages.table.subpages.schema.unique.label')" />
        </v-col>
        <v-col
          v-if="canRemove(idx)"
          cols="auto"
          class="mt-3 ml-5">
          <v-btn
            v-if="!frozen"
            size="small"
            :color="disabled ? '' : 'error'"
            variant="flat"
            :disabled="disabled"
            :text="$t('pages.table.subpages.schema.remove.text')"
            @click="removeColumn(idx)" />
        </v-col>
      </v-row>
      <v-row
        dense>
        <v-col>
          <v-btn
            v-if="!frozen"
            size="small"
            :color="disabled ? '' : 'tertiary'"
            :variant="buttonVariant"
            :disabled="disabled"
            :loading="loadColumn"
            :text="$t('pages.table.subpages.schema.add.text')"
            @click="addColumn()" />
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          <v-btn
            color="secondary"
            variant="flat"
            size="small"
            :loading="loading"
            :disabled="disabled || !valid || columns.length === 0"
            :text="submitText"
            @click="submit" />
        </v-col>
      </v-row>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache.js'

export default {
  props: {
    columns: {
      type: Array,
      default () {
        return []
      }
    },
    disabled: {
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
    },
    submitText: {
      type: String,
      default() {
        return null
      }
    },
    frozen: {
      type: Boolean,
      default () {
        return false
      }
    }
  },
  data () {
    return {
      valid: false,
      columnTypes: [],
      loadColumn: false,
      loadingColumnTypes: false,
      tableColumns: [],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    showPrimaryKeyWarning () {
      if (this.disabled) {
        return false
      }
      return this.columns.filter(c => c.primary_key).length === 0
    }
  },
  watch: {
    valid: {
      handler () {
        this.$emit('schema-valid', { valid: this.valid })
      }
    }
  },
  mounted () {
    this.fetchColumnTypes()
  },
  methods: {
    fetchColumnTypes () {
      if (!this.database || this.columnTypes.length > 0) {
        return
      }
      this.loadingColumnTypes = true
      const imageService = useImageService()
      imageService.findById(this.database.container.image.id)
        .then((image) => {
          const types = image.data_types
          if (this.columns.filter(c => c.type === 'serial').length > 0) {
            this.columnTypes = types.filter(t => t.value !== 'serial')
          } else {
            this.columnTypes = types
          }
          this.loadingColumnTypes = false
        })
        .catch(({code, message}) => {
          this.loadingColumnTypes = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingColumnTypes = false
        })
    },
    submit () {
      const tableService = useTableService()
      this.$emit('close', { success: true, columns: tableService.prepareColumns(this.columns), constraints: tableService.prepareConstraints(this.columns) })
    },
    setOthers (column) {
      column.null_allowed = false
      column.unique = true
    },
    canRemove (idx) {
      if (idx > 0) {
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
    addColumn (name = '', type = null, null_allowed = true, primary_key = false, unique = false) {
      this.loadColumn = true
      const column = {
        name,
        type,
        null_allowed,
        primary_key,
        sets: [],
        sets_values: null,
        enums: [],
        enums_values: null,
        d: 0
      }
      column.size = this.columnType(column).size_required === true ? this.columnType(column).size_default : null
      this.columns.push(column)
      this.$refs.form.validate()
      this.loadColumn = false
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
    columnType (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      return filter[0]
    },
    sizeHint (column) {
      let hint = ''
      if (this.columnType(column).size_min !== null) {
        hint += `min. ${this.columnType(column).size_min}`
      }
      if (this.columnType(column).size_max) {
        if (hint.length > 0) {
          hint += ', '
        }
        hint += `max. ${this.columnType(column).size_max}`
      }
      if (!this.columnType(column).size_required) {
        hint += ' (optional)'
      }
      return hint
    },
    dHint (column) {
      let hint = ''
      if (this.columnType(column).d_min !== null) {
        hint += `min. ${this.columnType(column).d_min}`
      }
      if (this.columnType(column).d_max) {
        if (hint.length > 0) {
          hint += ', '
        }
        hint += `max. ${this.columnType(column).d_max}`
      }
      if (!this.columnType(column).d_required) {
        hint += ' (optional)'
      }
      return hint
    },
    setDefaultSizeAndD (column) {
      if (this.columnType(column).size_default !== null) {
        column.size = this.columnType(column).size_default
      } else {
        column.size = null
      }
      if (this.columnType(column).d_default !== null) {
        column.d = this.columnType(column).d_default
      } else {
        column.d = null
      }
      console.debug('for column type', column.type, 'set default size', column.size, '& d', column.d)
      if (column.type === 'serial') {
        this.setOthers(column)
      }
    },
    hasEnumOrSet (column) {
      return column.type === 'enum' || column.type === 'set'
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
