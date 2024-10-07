<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      :disabled="disabled">
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
            :rules="[v => !!v || $t('validation.required')]"
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
            item-title="text"
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
            :rules="[v => !!v || $t('validation.required')]"
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
          v-if="defaultSize(c) !== false || hasMinSize(c) || hasMaxSize(c)"
          cols="1">
          <v-text-field
            v-model.number="c.size"
            type="number"
            required
            :min="hasMinSize(c) ? minSize(c) : null"
            :max="hasMaxSize(c) ? maxSize(c) : null"
            :step="sizeSteps(c)"
            :hint="sizeHint(c)"
            :clearable="!optionalSize(c)"
            persistent-hint
            :variant="inputVariant"
            :rules="[v => !(!defaultSize(c) && (v === null || v === '')) || $t('validation.required')]"
            :error-messages="sizeErrorMessages(c)"
            :label="$t('pages.table.subpages.schema.size.label')" />
        </v-col>
        <v-col
          v-if="defaultD(c) !== false"
          cols="1">
          <v-text-field
            v-model.number="c.d"
            type="number"
            required
            :variant="inputVariant"
            :rules="[v => (v !== null && v !== '') || $t('validation.required')]"
            :error-messages="dErrorMessages(c)"
            :label="$t('pages.table.subpages.schema.d.label')" />
        </v-col>
        <v-col
          v-if="shift(c)"
          :cols="shift(c)" />
        <v-col
          cols="auto"
          class="pl-2">
          <v-checkbox
            v-model="c.primary_key"
            :disabled="disabled"
            :label="$t('pages.table.subpages.schema.primary-key.label')"
            @click="setOthers(c)" />
        </v-col>
        <v-col
          cols="auto"
          class="pl-10">
          <v-checkbox
            v-model="c.null_allowed"
            :disabled="c.primary_key || disabled"
            :label="$t('pages.table.subpages.schema.null.label')" />
        </v-col>
        <v-col
          cols="auto"
          class="pl-10">
          <v-checkbox
            v-model="c.unique"
            :disabled="disabled"
            :hidden="c.primary_key"
            :label="$t('pages.table.subpages.schema.unique.label')" />
        </v-col>
        <v-col
          v-if="canRemove(idx)"
          cols="auto"
          class="mt-3 ml-5">
          <v-btn
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
            size="small"
            :color="disabled ? '' : 'tertiary'"
            :variant="buttonVariant"
            :disabled="disabled"
            :text="$t('pages.table.subpages.schema.add.text')"
            @click="addColumn()" />
        </v-col>
      </v-row>
      <v-row
        v-if="showPrimaryKeyWarning">
        <v-col md="8">
          <v-alert
            border="start"
            color="warning">
            {{ $t('pages.table.subpages.import.schema.primary.warn') }}
          </v-alert>
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          <v-btn
            color="secondary"
            variant="flat"
            size="small"
            :loading="loading"
            :disabled="disabled || !valid || showPrimaryKeyWarning || this.columns.length === 0"
            :text="submitText"
            @click="submit" />
        </v-col>
      </v-row>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'

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
      default () {
        return null
      }
    }
  },
  data () {
    return {
      valid: false,
      tableColumns: [],
      columnTypes: useQueryService().mySql8DataTypes(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    dateFormats () {
      if (!this.database || !('container' in this.database) || !('image' in this.database.container) || !('date_formats' in this.database.container.image)) {
        return []
      }
      return this.database.container.image.date_formats
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
  methods: {
    shift (column) {
      if (!this.columns || this.columns.length === 0) {
        return false
      }
      let shift = 0
      if (this.hasDate(column) === false && this.columns.filter(c => this.hasDate(c) !== false).length > 0) {
        shift++
      }
      if (this.defaultSize(column) === false && this.columns.filter(c => this.defaultSize(c) !== false).length > 0) {
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
      this.columns.push({
        name,
        type,
        null_allowed,
        primary_key,
        sets: [],
        sets_values: null,
        enums: [],
        enums_values: null,
        size: 0,
        d: 0
      })
      this.$refs.form.validate()
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
    requiredSize (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      if (filter[0].requiredSize === undefined || filter[0].requiredSize === null) {
        return false
      }
      return filter[0].requiredSize
    },
    hasMinSize (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      return filter[0].minSize !== undefined
    },
    minSize (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      if (filter[0].minSize === undefined || filter[0].minSize === null) {
        return false
      }
      return filter[0].minSize
    },
    hasMaxSize (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      return filter[0].maxSize !== undefined
    },
    maxSize (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return false
      }
      if (filter[0].maxSize === undefined || filter[0].maxSize === null) {
        return false
      }
      return filter[0].maxSize
    },
    sizeSteps (column) {
      const filter = this.columnTypes.filter(t => t.value === column.type)
      if (!filter || filter.length === 0) {
        return null
      }
      if (filter[0].sizeSteps === undefined || filter[0].sizeSteps === null) {
        return 1
      }
      return filter[0].sizeSteps
    },
    sizeHint (column) {
      let hint = ''
      if (this.hasMinSize(column)) {
        hint += `min. ${this.minSize(column)}`
      }
      if (this.hasMaxSize(column)) {
        if (hint.length > 0) {
          hint += ', '
        }
        hint += `max. ${this.maxSize(column)}`
      }
      if (!this.defaultSize(column)) {
        hint += ' (optional)'
      }
      return hint
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
      console.debug('for column type', column.type, 'set default size', column.size, '& d', column.d)
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
