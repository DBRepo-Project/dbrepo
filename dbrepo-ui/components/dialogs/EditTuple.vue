<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      @submit.prevent="submit">
      <v-card
        :title="title"
        :subtitle="this.$t('toolbars.table.data.subtitle')"
        variant="elevated">
        <v-card-text>
          <v-row
            v-for="(column, idx) in table.columns"
            :key="`c-${idx}`"
            dense>
            <v-col>
              <v-text-field
                v-if="isNumber(column)"
                v-model.number="tuple[column.internal_name]"
                :disabled="!edit"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                :rules="rules(column)"
                :required="required(column)"
                type="number">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-text-field>
              <v-text-field
                v-if="isTextField(column)"
                v-model="tuple[column.internal_name]"
                :disabled="disabled(column)"
                :clearable="!required(column)"
                :counter="maxLength(column) !== null"
                :maxlength="maxLength(column)"
                :rules="rules(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                type="text">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-text-field>
              <v-text-field
                v-if="isFloatingPoint(column)"
                v-model="tuple[column.internal_name]"
                :disabled="disabled(column)"
                step=".1"
                :clearable="!required(column)"
                :rules="rules(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                type="number">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-text-field>
              <v-textarea
                v-if="isTextArea(column)"
                v-model="tuple[column.internal_name]"
                :disabled="disabled(column)"
                rows="3"
                :clearable="!required(column)"
                :rules="rules(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-textarea>
              <BlobUpload
                v-if="isFileField(column)"
                :column="column"
                @blob="onUpload" />
              <v-select
                v-if="isSet(column) || isEnum(column)"
                v-model="tuple[column.internal_name]"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                :rules="rules(column)"
                :required="required(column)"
                :clearable="!required(column)"
                :items="isSet(column) ? column.sets : column.enums">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-select>
              <v-select
                v-if="isBoolean(column)"
                v-model="tuple[column.internal_name]"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                :rules="rules(column)"
                :required="required(column)"
                :items="bools"
                :clearable="!required(column)">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-select>
              <v-text-field
                v-if="isTimeField(column)"
                v-model="tuple[column.internal_name]"
                :clearable="!required(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)">
                <template
                  v-slot:append>
                  {{ column.column_type.toUpperCase() }}
                  <NuxtLink
                    target="_blank"
                    class="ml-2"
                    :href="documentationLink(column)">
                    <v-tooltip
                      location="bottom">
                      <template
                        v-slot:activator="{ props }">
                        <v-icon
                          v-bind="props"
                          icon="mdi-help-circle-outline" />
                      </template>
                      {{ $t('navigation.help') }}
                    </v-tooltip>
                  </NuxtLink>
                </template>
              </v-text-field>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            :variant="buttonVariant"
            :text="$t('navigation.cancel')"
            @click="cancel" />
          <v-btn
            v-if="!edit"
            id="addTuple"
            variant="flat"
            :disabled="!valid || loading"
            :loading="loading"
            color="primary"
            type="submit"
            :text="$t('pages.database.subpages.tuple.create.text')"
            @click="addTuple" />
          <v-btn
            v-if="edit"
            id="updateTuple"
            variant="flat"
            :disabled="!valid || loading"
            :loading="loading"
            color="primary"
            type="submit"
            :text="$t('pages.database.subpages.tuple.update.text')"
            @click="updateTuple" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import BlobUpload from '@/components/table/BlobUpload.vue'

export default {
  components: {
    BlobUpload
  },
  props: {
    tuple: {
      type: Object,
      default: () => {
        return null
      }
    },
    edit: {
      type: Boolean,
      default: () => {
        return false
      }
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
      oldTuple: null,
      error: false,
      menu: false,
      bools: [
        { title: 'true', value: true },
        { title: 'false', value: false }
      ],
      cacheStore: useCacheStore()
    }
  },
  mounted() {
    this.$refs.form.validate()
    this.oldTuple = Object.assign({}, this.tuple)
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    columnTypes () {
      if (!this.database) {
        return []
      }
      return this.database.container.image.data_types
    },
    title () {
      return (this.edit ? this.$t('toolbars.table.data.edit') : this.$t('toolbars.table.data.add')) + ' ' + this.$t('toolbars.table.data.tuple')
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
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
      const { is_null_allowed, is_primary_key } = column
      let hint = ''
      if (!is_null_allowed) {
        hint += this.$t('pages.table.subpages.data.required.hint')
      }
      if (column.column_type === 'sequence') {
        hint += ' ' + this.$t('pages.table.subpages.data.auto.hint')
      }
      if (is_primary_key) {
        hint += ' ' + this.$t('pages.table.subpages.data.primary-key.hint')
      }
      if (this.formatHint(column)) {
        hint += this.$t('pages.table.subpages.data.format.hint') + ' ' + this.formatHint(column)
      }
      return hint
    },
    documentationLink ({column_type}) {
      const filter = this.columnTypes.filter(t => t.value === column_type)
      if (filter.length !== 1) {
        return null
      }
      return filter[0].documentation
    },
    formatHint ({column_type}) {
      const filter = this.columnTypes.filter(t => t.value === column_type)
      if (filter.length !== 1) {
        return null
      }
      return filter[0].data_hint
    },
    isTextField (column) {
      const { column_type } = column
      return ['char', 'varchar', 'tinytext', 'mediumtext'].includes(column_type)
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
      if (column.is_null_allowed) {
        return []
      }
      const rules = []
      rules.push(v => v !== null || this.$t('validation.required'))
      if (column.column_type === 'decimal' || column.column_type === 'double') {
        rules.push(v => !(!v || v.split('.')[0].length > column.size) || `${this.$t('pages.table.subpages.data.float.max')} ${column.size} ${this.$t('pages.table.subpages.data.float.before')}`)
        rules.push(v => !(!v || (column.d && v.split('.')[1].length > column.d)) || `${this.$t('pages.table.subpages.data.float.max')} ${column.d} ${this.$t('pages.table.subpages.data.float.after')}`)
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
      return (this.edit && column.is_primary_key) || !this.edit
    },
    updateTuple () {
      const constraints = {}
      if (this.table.constraints.primary_key.length > 0) {
        this.table.constraints.primary_key
          .forEach((pk) => {
            constraints[pk.column.internal_name] = this.oldTuple[pk.column.internal_name]
          })
        console.debug('table has primary key: set update tuple constraints', constraints)
      } else {
        this.table.columns
          .forEach((column) => {
            constraints[column.internal_name] = this.oldTuple[column.internal_name]
          })
        console.debug('table does not have a primary key: set update tuple constraints', constraints)
      }
      const tupleService = useTupleService()
      this.loading = true
      tupleService.update(this.$route.params.database_id, this.$route.params.table_id, { data: this.tuple, keys: constraints })
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.data.update'))
          this.$emit('close', { success: true })
          this.loading = false
        })
        .catch(({message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(message)
        })
        .finally(() => {
          this.loading = false
        })
    },
    addTuple () {
      const constraints = {}
      this.table.columns
        .filter(c => c.is_primary_key)
        .forEach((c) => {
          constraints[c.internal_name] = this.tuple[c.internal_name]
        })
      this.table.columns.forEach((column) => {
        if (!(column.internal_name in this.tuple)) {
          this.tuple[column.internal_name] = null
        }
      })
      const tupleService = useTupleService()
      this.loading = true
      tupleService.create(this.$route.params.database_id, this.$route.params.table_id, { data: this.tuple })
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.data.add'))
          this.$emit('close', { success: true })
          this.loading = false
        })
        .catch(({message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(message)
        })
        .finally(() => {
          this.loading = false
        })
    },
    onUpload ({column, s3key}) {
      const toast = useToastInstance()
      toast.success(this.$t('success.upload.blob'))
      this.tuple[column.internal_name] = s3key
    }
  }
}
</script>
