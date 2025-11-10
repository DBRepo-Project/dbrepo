<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
      @submit.prevent="validate">
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
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                :required="required(column)"
                type="number">
                <template
                  v-slot:append>
                  {{ column.type.toUpperCase() }}
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
                :clearable="!required(column)"
                :counter="maxLength(column) !== null"
                :maxlength="maxLength(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)"
                type="text">
                <template
                  v-slot:append>
                  {{ column.type.toUpperCase() }}
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
                step=".1"
                :clearable="!required(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :error-messages="floatingPointErrors(column, tuple[column.internal_name])"
                :hint="hint(column)"
                type="number">
                <template
                  v-slot:append>
                  {{ column.type.toUpperCase() }}
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
                rows="3"
                :clearable="!required(column)"
                :required="required(column)"
                persistent-hint
                :variant="inputVariant"
                :label="column.internal_name"
                :hint="hint(column)">
                <template
                  v-slot:append>
                  {{ column.type.toUpperCase() }}
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
                :required="required(column)"
                :clearable="!required(column)"
                :items="isSet(column) ? column.sets : column.enums">
                <template
                  v-slot:append>
                  {{ column.type.toUpperCase() }}
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
                :required="required(column)"
                :items="bools"
                :clearable="!required(column)">
                <template
                  v-slot:append>
                  {{ column.type.toUpperCase() }}
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
                  {{ column.type.toUpperCase() }}
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
      loadContainer: false,
      container: null,
      bools: [
        { title: 'true', value: true },
        { title: 'false', value: false }
      ],
      cacheStore: useCacheStore()
    }
  },
  mounted () {
    this.fetchContainer()
    this.oldTuple = Object.assign({}, this.tuple)
    this.validate()
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    table () {
      return this.cacheStore.getTable
    },
    columnTypes () {
      if (!this.container) {
        return []
      }
      return this.container.image.data_types
    },
    primaryKeyColumns () {
      if (!this.table) {
        return []
      }
      return this.table.constraints.primary_key.map(pk => pk.column)
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
  watch: {
    tuple: {
      handler () {
        this.validate()
      },
      deep: true
    }
  },
  methods: {
    validate () {
      console.debug('validate form')
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
      if (column.type === 'sequence') {
        hint += ' ' + this.$t('pages.table.subpages.data.auto.hint')
      }
      if (is_primary_key) {
        hint += ' ' + this.$t('pages.table.subpages.data.primary_key.hint')
      }
      if (this.formatHint(column)) {
        hint += this.$t('pages.table.subpages.data.format.hint') + ' ' + this.formatHint(column)
      }
      return hint
    },
    documentationLink ({type}) {
      const filter = this.columnTypes.filter(t => t.value === type)
      if (filter.length !== 1) {
        return null
      }
      return filter[0].documentation
    },
    formatHint ({type}) {
      const filter = this.columnTypes.filter(t => t.value === type)
      if (filter.length !== 1) {
        return null
      }
      return filter[0].data_hint
    },
    isTextField (column) {
      const { type } = column
      return ['char', 'varchar', 'tinytext', 'mediumtext'].includes(type)
    },
    isTextArea (column) {
      return ['text'].includes(column.type)
    },
    isFileField (column) {
      return ['blob', 'longblob', 'mediumblob', 'tinyblob'].includes(column.type)
    },
    isBoolean (column) {
      return ['bool'].includes(column.type)
    },
    isNumber (column) {
      return ['int', 'binary', 'bit', 'tinyint', 'smallint', 'mediumint', 'bigint', 'serial'].includes(column.type)
    },
    isFloatingPoint (column) {
      return ['float', 'double', 'decimal'].includes(column.type)
    },
    isEnum (column) {
      return column.type === 'enum'
    },
    isSet (column) {
      return column.type === 'set'
    },
    isTimeField (column) {
      return ['date', 'datetime', 'timestamp', 'time', 'year'].includes(column.type)
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
    updateTuple () {
      const constraints = {}
      this.primaryKeyColumns
        .forEach((pk) => {
          constraints[pk.internal_name] = this.oldTuple[pk.internal_name]
        })
      console.debug('table has primary key: set update tuple constraints', constraints)
      const tupleService = useTupleService()
      this.loading = true
      tupleService.update(this.$route.params.database_id, this.$route.params.table_id, { data: this.tuple, keys: constraints })
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.data.update'))
          this.$emit('close', { success: true })
          this.loading = false
        })
        .catch(({code, message}) => {
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
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    },
    onUpload ({column, s3key}) {
      const toast = useToastInstance()
      toast.success(this.$t('success.upload.blob'))
      this.tuple[column.internal_name] = s3key
    },
    fetchContainer () {
      if (!this.database) {
        return
      }
      this.loadContainer = true
      const containerService = useContainerService()
      containerService.findOne(this.database.container.id)
        .then((container) => {
          this.container = container
          this.loadContainer = false
        })
        .catch(({code, message}) => {
          this.loadContainer = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadContainer = false
        })
    },
    floatingPointErrors (column, value) {
      if (!value && column.null_allowed) {
        return null
      } else if (!value && !column.null_allowed) {
        return this.$t('validation.required')
      }
      const beforeComma = value.substring(0, value.indexOf('.') - 1)
      const afterComma = value.substring(value.indexOf('.') + 1, value.length)
      if (beforeComma.length > column.size) {
        return this.$t('validation.size', {'size': column.size})
      }
      if (afterComma.length > column.d) {
        return this.$t('validation.d', {'size': column.d})
      }
      return null
    }
  }
}
</script>
