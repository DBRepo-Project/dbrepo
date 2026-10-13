<template>
  <div
    class="mt-4">
    <v-row
      v-for="(foreignKey, index) in localForeignKeys"
      :key="`fk-${index}`"
      class="foreign-key pa-2 ml-1 mr-1 mb-2"
      dense>
      <v-col
        cols="12"
        md="4">
        <v-combobox
          :model-value="foreignKey.columns"
          multiple
          chips
          closable-chips
          :items="localColumnOptions"
          :disabled="disabled"
          :variant="inputVariant"
          :label="$t('pages.table.subpages.schema.foreign_keys.columns.label')"
          :hint="$t('pages.table.subpages.schema.foreign_keys.columns.hint')"
          :rules="[v => v.length > 0 || $t('validation.required')]"
          persistent-hint
          @update:modelValue="updateColumns(index, $event)" />
      </v-col>
      <v-col
        cols="12"
        md="4">
        <v-select
          :model-value="selectedReference(foreignKey)"
          :items="referenceOptions(foreignKey)"
          item-title="title"
          return-object
          :disabled="disabled || foreignKey.columns.length === 0"
          :variant="inputVariant"
          :no-data-text="$t('pages.table.subpages.schema.foreign_keys.references.empty')"
          :label="$t('pages.table.subpages.schema.foreign_keys.references.label')"
          :rules="[v => !!v || $t('validation.required')]"
          persistent-hint
          @update:modelValue="updateReference(index, $event)" />
      </v-col>
      <v-col
        cols="12"
        md="3">
        <div class="text-caption mb-1">
          {{ $t('pages.table.subpages.schema.foreign_keys.selected.label') }}
        </div>
        <div
          v-if="foreignKey.columns.length === 0"
          class="text-medium-emphasis text-caption">
          {{ $t('pages.table.subpages.schema.foreign_keys.selected.empty') }}
        </div>
        <v-chip
          v-for="(column, columnIndex) in foreignKey.columns"
          :key="`fk-${index}-column-${column}`"
          class="mr-1 mb-1"
          size="small">
          {{ column }}
          <v-btn
            class="ml-1"
            density="compact"
            size="x-small"
            variant="text"
            icon="mdi-arrow-up"
            :aria-label="$t('pages.table.subpages.schema.foreign_keys.move.up')"
            :title="$t('pages.table.subpages.schema.foreign_keys.move.up')"
            :disabled="disabled || columnIndex === 0"
            @click="moveColumn(index, columnIndex, -1)" />
          <v-btn
            density="compact"
            size="x-small"
            variant="text"
            icon="mdi-arrow-down"
            :aria-label="$t('pages.table.subpages.schema.foreign_keys.move.down')"
            :title="$t('pages.table.subpages.schema.foreign_keys.move.down')"
            :disabled="disabled || columnIndex === foreignKey.columns.length - 1"
            @click="moveColumn(index, columnIndex, 1)" />
        </v-chip>
      </v-col>
      <v-col
        cols="auto"
        class="mt-3">
        <v-btn
          size="small"
          :color="disabled ? '' : 'error'"
          variant="flat"
          :disabled="disabled"
          :text="$t('pages.table.subpages.schema.foreign_keys.remove.text')"
          @click="removeForeignKey(index)" />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col>
        <v-btn
          size="small"
          :color="disabled ? '' : 'tertiary'"
          :variant="buttonVariant"
          :disabled="disabled || localColumnOptions.length === 0"
          :loading="loading"
          :text="$t('pages.table.subpages.schema.foreign_keys.add.text')"
          @click="addForeignKey" />
      </v-col>
    </v-row>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: {
      type: Array,
      default () {
        return []
      }
    },
    columns: {
      type: Array,
      default () {
        return []
      }
    },
    tables: {
      type: Array,
      default () {
        return []
      }
    },
    currentTableName: {
      type: String,
      default () {
        return null
      }
    },
    disabled: {
      type: Boolean,
      default () {
        return false
      }
    },
    frozen: {
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
    inputVariant: {
      type: String,
      default () {
        return 'underlined'
      }
    },
    buttonVariant: {
      type: String,
      default () {
        return 'flat'
      }
    }
  },
  emits: ['update:modelValue'],
  data () {
    return {
      localForeignKeys: this.cloneForeignKeys(this.modelValue),
      syncing: false
    }
  },
  computed: {
    localColumnOptions () {
      return this.columns
        .map(column => column.name)
        .filter(column => !!column)
    },
    selfPrimaryKeyColumns () {
      return this.columns
        .filter(column => column.primary_key)
        .map(column => {
          return {
            name: column.name,
            internal_name: column.name,
            type: column.type,
            column_type: column.type,
            size: column.size,
            d: column.d
          }
        })
    }
  },
  watch: {
    modelValue: {
      deep: true,
      handler (value) {
        if (this.syncing) {
          return
        }
        this.localForeignKeys = this.cloneForeignKeys(value)
      }
    },
    columns: {
      deep: true,
      handler () {
        this.normalizeForeignKeys()
      }
    },
    currentTableName () {
      this.normalizeForeignKeys()
    }
  },
  methods: {
    addForeignKey () {
      this.localForeignKeys.push(this.emptyForeignKey())
      this.emitForeignKeys()
    },
    removeForeignKey (index) {
      this.localForeignKeys.splice(index, 1)
      this.emitForeignKeys()
    },
    updateColumns (index, columns) {
      const foreignKey = this.localForeignKeys[index]
      foreignKey.columns = this.sanitizeColumns(columns)
      this.clearInvalidReference(foreignKey)
      this.emitForeignKeys()
    },
    updateReference (index, reference) {
      const foreignKey = this.localForeignKeys[index]
      if (!reference) {
        foreignKey.referenced_table = null
        foreignKey.referenced_columns = []
        this.emitForeignKeys()
        return
      }
      foreignKey.referenced_table = reference.referenced_table
      foreignKey.referenced_columns = reference.referenced_columns
      this.emitForeignKeys()
    },
    moveColumn (foreignKeyIndex, columnIndex, direction) {
      const foreignKey = this.localForeignKeys[foreignKeyIndex]
      const nextIndex = columnIndex + direction
      const columns = [...foreignKey.columns]
      const column = columns.splice(columnIndex, 1)[0]
      columns.splice(nextIndex, 0, column)
      foreignKey.columns = columns
      this.clearInvalidReference(foreignKey)
      this.emitForeignKeys()
    },
    normalizeForeignKeys () {
      let changed = false
      this.localForeignKeys.forEach(foreignKey => {
        const columns = this.sanitizeColumns(foreignKey.columns)
        if (columns.join('|') !== foreignKey.columns.join('|')) {
          foreignKey.columns = columns
          changed = true
        }
        if (this.clearInvalidReference(foreignKey)) {
          changed = true
        }
      })
      if (changed) {
        this.emitForeignKeys()
      }
    },
    clearInvalidReference (foreignKey) {
      if (!foreignKey.referenced_table || foreignKey.referenced_columns.length === 0) {
        return false
      }
      const reference = this.referenceOptions(foreignKey)
        .find(option => this.sameReference(option, foreignKey))
      if (reference) {
        return false
      }
      foreignKey.referenced_table = null
      foreignKey.referenced_columns = []
      return true
    },
    referenceOptions (foreignKey) {
      if (!foreignKey || foreignKey.columns.length === 0) {
        return []
      }
      const references = []
      this.tables.forEach(table => {
        const primaryKeyColumns = this.primaryKeyColumns(table)
        if (this.isEligibleReference(foreignKey.columns, primaryKeyColumns)) {
          references.push(this.referenceOption(table.internal_name || table.name, primaryKeyColumns))
        }
      })
      if (this.currentTableName && this.isEligibleReference(foreignKey.columns, this.selfPrimaryKeyColumns)) {
        references.push(this.referenceOption(this.currentTableName, this.selfPrimaryKeyColumns))
      }
      return references
    },
    selectedReference (foreignKey) {
      if (!foreignKey.referenced_table || foreignKey.referenced_columns.length === 0) {
        return null
      }
      return this.referenceOptions(foreignKey)
        .find(option => this.sameReference(option, foreignKey)) || null
    },
    referenceOption (tableName, primaryKeyColumns) {
      const referencedColumns = primaryKeyColumns.map(column => this.columnInternalName(column))
      return {
        title: `${tableName}(${referencedColumns.join(',')})`,
        referenced_table: tableName,
        referenced_columns: referencedColumns
      }
    },
    primaryKeyColumns (table) {
      if (!table || !table.constraints || !table.constraints.primary_key) {
        return []
      }
      return table.constraints.primary_key
        .map(primaryKey => {
          if (primaryKey && primaryKey.column) {
            return primaryKey.column
          }
          if (primaryKey && primaryKey.internal_name) {
            return primaryKey
          }
          return this.tableColumn(table, primaryKey)
        })
        .filter(column => !!column)
    },
    tableColumn (table, primaryKey) {
      if (!table.columns) {
        return null
      }
      if (typeof primaryKey === 'string') {
        return table.columns.find(column => column.internal_name === primaryKey || column.name === primaryKey)
      }
      return table.columns.find(column => column.id === primaryKey.column_id || column.id === primaryKey.id)
    },
    isEligibleReference (localColumns, primaryKeyColumns) {
      if (primaryKeyColumns.length === 0 || localColumns.length !== primaryKeyColumns.length) {
        return false
      }
      return localColumns.every((localColumnName, index) => {
        const localColumn = this.columns.find(column => column.name === localColumnName)
        const referencedColumn = primaryKeyColumns[index]
        return this.columnsCompatible(localColumn, referencedColumn)
      })
    },
    columnsCompatible (localColumn, referencedColumn) {
      if (!localColumn || !referencedColumn) {
        return false
      }
      if (this.columnType(localColumn) !== this.columnType(referencedColumn)) {
        return false
      }
      if (!this.metadataCompatible(localColumn.size, referencedColumn.size)) {
        return false
      }
      return this.metadataCompatible(localColumn.d, referencedColumn.d)
    },
    columnType (column) {
      return (column.column_type || column.type || '').toString().toLowerCase()
    },
    metadataCompatible (localValue, referencedValue) {
      const local = this.normalizeNumber(localValue)
      const referenced = this.normalizeNumber(referencedValue)
      if (local === null || referenced === null) {
        return true
      }
      return local === referenced
    },
    normalizeNumber (value) {
      if (value === null || typeof value === 'undefined' || value === '') {
        return null
      }
      return Number(value)
    },
    columnInternalName (column) {
      return column.internal_name || column.name
    },
    sameReference (option, foreignKey) {
      return option.referenced_table === foreignKey.referenced_table &&
        option.referenced_columns.join('|') === foreignKey.referenced_columns.join('|')
    },
    sanitizeColumns (columns) {
      const selected = Array.isArray(columns) ? columns : []
      return selected
        .filter(column => this.localColumnOptions.includes(column))
        .filter((column, index, all) => all.indexOf(column) === index)
    },
    emptyForeignKey () {
      return {
        columns: [],
        referenced_table: null,
        referenced_columns: [],
        on_update: 'no_action',
        on_delete: 'no_action'
      }
    },
    cloneForeignKeys (foreignKeys) {
      return foreignKeys.map(foreignKey => {
        return {
          columns: [...(foreignKey.columns || [])],
          referenced_table: foreignKey.referenced_table || null,
          referenced_columns: [...(foreignKey.referenced_columns || [])],
          on_update: foreignKey.on_update || 'no_action',
          on_delete: foreignKey.on_delete || 'no_action'
        }
      })
    },
    emitForeignKeys () {
      this.syncing = true
      this.$emit('update:modelValue', this.cloneForeignKeys(this.localForeignKeys))
      this.$nextTick(() => {
        this.syncing = false
      })
    }
  }
}
</script>

<style scoped>
.foreign-key {
  border: 1px solid rgb(var(--v-theme-surface-variant));
}
</style>
