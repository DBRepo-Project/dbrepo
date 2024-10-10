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
          :disabled="loadingFields"
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
                :loading="loadingFields"
                :disabled="loadingFields"
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
          <v-row
            v-if="!loading"
            dense>
            <v-col
              v-for="field in renderedFields"
              :key="`f-${field.attr_name}`"
              cols="3">
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
                :items="licenses"
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
              <p>
                {{ $t('pages.search.publication-range.hint') }}
              </p>
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
                :rules="[
                  v => !!v || $t('validation.required')
                ]"
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
                class="mt-4">
                {{ $t('pages.search.concept-unit.hint') }}
              </p>
            </v-col>
          </v-row>
          <v-row
            v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch"
            dense>
            <v-col
              v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch"
              cols="3">
              <v-select
                v-model="advancedSearchData['tables.columns.concept.uri']"
                clearable
                :items="concepts"
                item-title="name"
                item-value="uri"
                :variant="inputVariant"
                persistent-hint
                :loading="loadingConcepts"
                :label="$t('pages.search.concept.label')"
                :hint="$t('pages.search.concept.hint')" />
            </v-col>
            <v-col
              v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch"
              cols="3">
              <v-select
                v-model="advancedSearchData['tables.columns.unit.uri']"
                clearable
                :items="units"
                item-title="name"
                item-value="uri"
                :variant="inputVariant"
                persistent-hint
                :loading="loadingUnits"
                :label="$t('pages.search.unit.label')"
                :hint="$t('pages.search.unit.hint')" />
            </v-col>
            <v-col
              v-if="isEligibleUnitIndependentSearch"
              cols="3">
              <v-text-field
                v-model="advancedSearchData['t1']"
                clearable
                :variant="inputVariant"
                type="number"
                persistent-hint
                :label="$t('pages.search.start.label')"
                :hint="$t('pages.search.start.hint')" />
            </v-col>
            <v-col
              v-if="isEligibleUnitIndependentSearch"
              cols="3">
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
          <v-row
            dense>
            <v-col>
              <v-btn
                type="submit"
                color="secondary"
                variant="flat"
                :loading="loading"
                :disabled="!valid || loading || loadingFields"
                size="small"
                @click="advancedSearch">
                {{ $t('navigation.search') }}
              </v-btn>
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
      loadingConcepts: false,
      loadingUnits: false,
      loadingFields: false,
      showAdvancedSearch: false,
      concepts: [],
      units: [],
      licenses: [],
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
        identifier: ['identifiers.database_id', 'identifiers.query_id', 'identifiers.view_id', 'identifiers.table_id',
          'identifiers.publisher', 'identifiers.doi', 'identifiers.publication_year', 'identifiers.creator.username',
          'identifiers.licenses.uri', 'identifiers.funders.funder_identifier'],
        view: [],
        concept: ['tables.columns.concept.uri'],
        unit: ['tables.columns.unit.uri']
      },
      fieldItems: [
        { name: this.$t('pages.search.types.column'), value: 'column' },
        { name: this.$t('pages.search.types.concept'), value: 'concept' },
        { name: this.$t('pages.search.types.database'), value: 'database' },
        { name: this.$t('pages.search.types.identifier'), value: 'identifier' },
        { name: this.$t('pages.search.types.table'), value: 'table' },
        { name: this.$t('pages.search.types.unit'), value: 'unit' },
        { name: this.$t('pages.search.types.user'), value: 'user' },
        { name: this.$t('pages.search.types.view'), value: 'view' }
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
      if (!this.$route.query || !this.$route.query.type) {
        return null
      }
      return this.$route.query.type
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
    type: {
      /* from route */
      handler () {
        this.initStaticFields()
        this.initDynamicFields()
        if (this.searchType === 'column') {
          this.fetchConcepts()
          this.fetchUnits()
        }
      }
    },
    searchType: {
      /* from selection */
      handler () {
        this.initStaticFields()
        this.initDynamicFields()
        if (this.searchType === 'column') {
          this.fetchConcepts()
          this.fetchUnits()
        }
      }
    }
  },
  mounted () {
    this.initStaticFields()
    this.initDynamicFields()
    this.fetchLicenses()
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
      searchService.general_search(this.searchType, this.advancedSearchData)
        .then(({results, type}) => {
          this.$emit('search-result', {results, type})
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
      }
      return shouldBeRendered
    },
    fetchLicenses () {
      const licenseService = useLicenseService()
      licenseService.findAll()
        .then((licenses) => {
          this.licenses = licenses.map(l => l.identifier)
        })
    },
    fetchConcepts () {
      this.loadingConcepts = true
      const conceptService = useConceptService()
      conceptService.findAll()
        .then((response) => {
          this.concepts = conceptService.mapConcepts(response)
          this.loadingConcepts = false
        })
        .catch(() => {
          this.loadingConcepts = false
        })
        .finally(() => {
          this.loadingConcepts = false
        })
    },
    fetchUnits () {
      this.loadingUnits = true
      const unitService = useUnitService()
      unitService.findAll()
        .then((response) => {
          this.units = unitService.mapUnits(response)
          this.loadingUnits = false
        })
        .catch(() => {
          this.loadingUnits = false
        })
        .finally(() => {
          this.loadingUnits = false
        })
    },
    initDynamicFields () {
      if (!this.searchType || this.loadingFields) {
        return
      }
      this.resetAdvancedSearchFields()
      this.$emit('search-result', { results: [], type: this.searchType })
      const searchService = useSearchService()
      this.loadingFields = true
      searchService.fields(this.searchType)
        .then((response) => {
          this.renderedFields = response.filter(field => this.shouldRenderItem(field))
          console.debug('init dynamic attributes', this.renderedFields.map(f => f.attr_name))
          this.renderedFields.forEach((field) => {
            const filter = this.dynamicFields[this.searchType].filter(tuple => tuple.key === field.attr_name)
            if (filter.length > 0) {
              field.attr_friendly_name = filter[0].name
            }
          })
          this.loadingFields = false
        })
        .catch(() => {
          this.loadingFields = false
        })
    },
    initStaticFields () {
      if (this.type) {
        console.debug('init search type', this.type)
        this.searchType = this.type
      }
      const keys = Object.keys(this.$route.query)
        .filter(key => key !== 'type')
        .filter(key => this.dynamicFields[this.searchType].filter(dkey => key === dkey))
      console.debug('init static fields', keys)
      keys.forEach((key) => {
        this.advancedSearchData[key] = this.$route.query[key]
      })
    }
  }
}
</script>
