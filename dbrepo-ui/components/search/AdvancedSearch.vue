<template>
  <div>
    <v-card
      v-if="isAdvancedSearch"
      variant="flat">
      <v-card-text
        class="pt-2">
        <v-form
          ref="form"
          v-model="valid"
          autocomplete="off"
          @submit.prevent="submit">
          <v-row dense>
            <v-col cols="3">
              <v-select
                v-model="searchType"
                :items="fieldItems"
                item-title="name"
                item-value="value"
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.search.type.label')"
                :hint="$t('pages.search.type.hint')" />
            </v-col>
          </v-row>
          <v-row
            dense>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData.id"
                clearable
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.search.id.label')"
                :hint="$t('pages.search.id.hint')" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-if="!hideFields.hideNameField"
                v-model="advancedSearchData.name"
                clearable
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.search.name.label')"
                :hint="$t('pages.search.name.hint')" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-if="!hideFields.hideInternalNameField"
                v-model="advancedSearchData.internal_name"
                clearable
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.search.internal-name.label')"
                :hint="$t('pages.search.internal-name.hint')" />
            </v-col>
          </v-row>
          <v-row v-if="!loadingFields && renderedFields" dense>
            <v-col v-for="field in renderedFields" :key="`f-${field.attr_name}`" cols="3">
              <v-select
                v-if="field.type === 'boolean'"
                v-model="advancedSearchData[field.attr_name]"
                clearable
                :items="booleanItems"
                item-title="name"
                item-value="value"
                :variant="inputVariant"
                :label="field.attr_friendly_name" />
              <v-text-field
                v-if="(field.type === 'keyword' && field.attr_name !== 'column_type') || field.type === 'text' || field.type === 'date'"
                v-model="advancedSearchData[field.attr_name]"
                type="text"
                :variant="inputVariant"
                :label="field.attr_friendly_name"
                clearable />
              <v-select
                v-if="field.type === 'keyword' && field.attr_name === 'column_type'"
                v-model="advancedSearchData[field.attr_name]"
                :items="columnTypes"
                item-value="value"
                :variant="inputVariant"
                clearable
                :label="field.attr_friendly_name" />
              <v-text-field
                v-if="field.type.startsWith('integer') || field.type.startsWith('long') || field.type.startsWith('double')"
                v-model="advancedSearchData[field.attr_name]"
                type="number"
                :variant="inputVariant"
                :label="field.attr_friendly_name"
                clearable />
              <v-autocomplete
                v-if="field.attr_name === 'licenses'"
                v-model="advancedSearchData[field.attr_name]"
                :items="fetchLicenses"
                :variant="inputVariant"
                :label="field.attr_friendly_name"
                clearable
                multiple />
            </v-col>
          </v-row>
          <v-row
            v-if="isEligibleYearRangeSearch"
            dense>
            <v-col>
              <p v-text="$t('pages.search.publication-range.hint')" />
            </v-col>
          </v-row>
          <v-row
            v-if="isEligibleYearRangeSearch"
            dense>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData['t1']"
                type="number"
                persistent-hint
                :label="$t('pages.search.start-year.label')"
                :hint="$t('pages.search.start-year.hint')"
                :variant="inputVariant"
                required
                :rules="[v => !!v || $t('validation.required')]"
                clearable />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData['t2']"
                type="number"
                persistent-hint
                :label="$t('pages.search.end-year.label')"
                :hint="$t('pages.search.end-year.hint')"
                :variant="inputVariant"
                clearable />
            </v-col>
          </v-row>
          <v-row
            dense>
            <v-col>
              <p
                v-if="isEligibleUnitIndependentSearch"
                v-text="$t('pages.search.concept-unit.hint')"
                class="mt-4" />
            </v-col>
          </v-row>
          <v-row v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch" dense>
            <v-col v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch" cols="3">
              <v-select
                v-model="advancedSearchData['tables.columns.concept.uri']"
                clearable
                :items="concepts"
                item-title="name"
                item-value="uri"
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.search.concept.label')"
                :hint="$t('pages.search.concept.hint')" />
            </v-col>
            <v-col v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch" cols="3">
              <v-select
                v-model="advancedSearchData['tables.columns.unit.uri']"
                clearable
                :items="units"
                item-title="name"
                item-value="uri"
                :variant="inputVariant"
                persistent-hint
                :label="$t('pages.search.unit.label')"
                :hint="$t('pages.search.unit.hint')" />
            </v-col>
            <v-col v-if="isEligibleUnitIndependentSearch" cols="3">
              <v-text-field
                v-model="advancedSearchData['t1']"
                clearable
                :variant="inputVariant"
                type="number"
                persistent-hint
                :label="$t('pages.search.start.label')"
                :hint="$t('pages.search.start.hint')" />
            </v-col>
            <v-col v-if="isEligibleUnitIndependentSearch" cols="3">
              <v-text-field
                v-model="advancedSearchData['t2']"
                clearable
                :variant="inputVariant"
                type="number"
                persistent-hint
                :label="$t('pages.search.end.label')"
                :hint="$t('pages.search.end.hint')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-btn
                type="submit"
                color="secondary"
                variant="flat"
                :loading="loading"
                :disabled="!valid"
                size="small"
                :text="$t('navigation.search')"
                @click="advancedSearch" />
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      searchType: 'database',
      valid: false,
      loading: false,
      loadingFields: false,
      showAdvancedSearch: false,
      concepts: [],
      units: [],
      yearFrom: null,
      yearFromItems: [
        { name: `Since ${new Date().getFullYear()}`, value: new Date().getFullYear() },
        { name: `Since ${new Date().getFullYear() - 1}`, value: new Date().getFullYear() - 1 },
        { name: `Since ${new Date().getFullYear() - 2}`, value: new Date().getFullYear() - 2 },
        { name: `Since ${new Date().getFullYear() - 3}`, value: new Date().getFullYear() - 3 },
        { name: 'Custom', value: 'custom' }
      ],
      columnTypes: [],
      dynamicFields: {
        database: ['is_public', 'owner.attributes.orcid', 'owner.username', 'identifier.publication_year'],
        table: [],
        column: [],
        user: ['creator.firstname', 'creator.lastname', 'creator.username', 'creator.orcid'],
        identifier: [],
        view: [],
        concept: ['tables.columns.concept.uri'],
        unit: ['tables.columns.unit.uri']
      },
      fieldItems: [
        { name: 'Database', value: 'database' },
        { name: 'Table', value: 'table' },
        { name: 'Column', value: 'column' },
        { name: 'User', value: 'user' },
        { name: 'Identifier', value: 'identifier' },
        { name: 'Concept', value: 'concept' },
        { name: 'Unit', value: 'unit' },
        { name: 'View', value: 'view' }
      ],
      booleanItems: [
        { name: 'True', value: true },
        { name: 'False', value: false }
      ],
      fieldsResponse: null,
      renderedFields: [],
      advancedSearchData: {
        name: null,
        internal_name: null,
        id: null
      }
    }
  },
  computed: {
    hideFields () {
      const selectedOption = this.searchType
      return {
        hideNameField: selectedOption === 'identifier',
        hideInternalNameField: ['identifier', 'user', 'concept', 'unit'].includes(selectedOption)
      }
    },
    isEligibleConceptOrUnitSearch () {
      return ['column', 'database'].includes(this.searchType)
    },
    isEligibleUnitIndependentSearch () {
      return ['column'].includes(this.searchType)
    },
    isEligibleYearRangeSearch () {
      return this.searchType === 'database' && this.yearFrom === 'custom'
    },
    isAdvancedSearch () {
      return !this.$route.query.q
    },
    type () {
      if (!this.$route.query || !this.$route.query.t) {
        return null
      }
      return this.$route.query.t
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
    $route: {
      handler () {
        this.initFieldsFromRoute()
      }
    },
    type: {
      handler () {
        this.initFieldsFromRoute()
      }
    },
    searchType: {
      handler (newType, oldType) {
        if (!newType) {
          return
        }
        this.initSearch(newType)
        this.advancedSearch()
      },
      immediate: true
    }
  },
  mounted () {
    this.initFieldsFromRoute()
    this.initSearch(this.searchType)
    this.advancedSearch()
    const conceptService = useConceptService()
    conceptService.findAll()
      .then((response) => {
        this.concepts = conceptService.mapConcepts(response)
      })
    const unitService = useUnitService()
    unitService.findAll()
      .then((response) => {
        this.units = unitService.mapUnits(response)
      })
    const queryService = useQueryService()
    this.columnTypes = queryService.mySql8DataTypes().map((datatype) => {
      datatype.value = datatype.value.toUpperCase()
      return datatype
    })
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    /* Removes all advanced search fields when switching the type */
    resetAdvancedSearchFields () {
      Object.keys(this.advancedSearchData)
        .filter(k => !['name', 'internal_name', 'id'].includes(k))
        .filter(k => !Object.keys(this.$route.query).includes(k))
        .forEach((k) => {
          console.debug('delete advanced search key', k)
          delete this.advancedSearchData[k]
        })
    },
    advancedSearch () {
      console.debug('performing advanced search')
      if (this.search) {
        this.advancedSearchData.search_term = this.search
      } else {
        delete this.advancedSearchData.search_term
      }
      if ('t1' in this.advancedSearchData && this.advancedSearchData.t1) {
        this.advancedSearchData.t1 = Number(this.advancedSearchData.t1)
      }
      if ('t2' in this.advancedSearchData && this.advancedSearchData.t2) {
        this.advancedSearchData.t2 = Number(this.advancedSearchData.t2)
      }
      this.loading = true
      const searchService = useSearchService()
      searchService.search(this.searchType, this.advancedSearchData)
        .then((response) => {
          this.$emit('search-result', response)
        })
        .finally(() => {
          this.loading = false
        })
    },
    shouldRenderItem (item) {
      // Checks if item's attr_name matches any wanted field
      // The expected response is of a flattened format, so this method must be modified accordingly if the response is changed
      const possibleFields = this.dynamicFields[this.searchType]
      const shouldBeRendered = possibleFields.map(tuple => tuple).includes(item.attr_name)
      if (shouldBeRendered) {
        const attr = item.attr_name.substr(item.attr_name.lastIndexOf('.'), item.attr_name.length)
        console.debug('attribute', attr, 'should be rendered')
      }
      return shouldBeRendered
    },
    fetchLicenses () {
      // Licenses is a nested object in the backend, but without any values.
      // Instead, we define our custom license generator with a controlled vocabulary.
      return [
        'Apache-2.0', 'BSD-3-Clause', 'BSD-4-Clause', 'CC-BY-4.0', 'CC0-1.0', 'GPL-3.0-only', 'MIT'
      ]
    },
    initSearch (searchType) {
      this.resetAdvancedSearchFields()
      this.$emit('search-result', [])
      this.loadingFields = true
      const searchService = useSearchService()
      searchService.fields(searchType)
        .then((response) => {
          this.loadingFields = false
          this.renderedFields = response.filter(field => this.shouldRenderItem(field))
          this.renderedFields.forEach((field) => {
            const filter = this.dynamicFields[this.searchType].filter(tuple => tuple.key === field.attr_name)
            if (filter.length > 0) {
              field.attr_friendly_name = filter[0].name
            }
          })
        })
        .finally(() => {
          this.loadingFields = false
        })
    },
    initFieldsFromRoute () {
      if (this.type) {
        this.searchType = this.type
        console.debug('type', this.type, 'is present: set search type to', this.searchType)
      }
      const keys = Object.keys(this.$route.query).filter(key => key !== 't').filter(key => this.dynamicFields[this.searchType].filter(dkey => key === dkey))
      keys.forEach((key) => {
        this.advancedSearchData[key] = this.$route.query[key]
        console.debug('set advanced search field with key', key, 'to value', this.$route.query[key])
      })
    }
  }
}
</script>
