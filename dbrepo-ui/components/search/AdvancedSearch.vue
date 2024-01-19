<template>
  <div>
    <v-card v-if="isAdvancedSearch" flat tile>
      <v-card-text class="pt-0 pl-4 pb-6 pr-4">
        <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="3">
              <v-select
                v-model="searchType"
                :items="fieldItems"
                item-text="name"
                item-value="value"
                solo
                label="Type" />
            </v-col>
          </v-row>
          <p>The following fields are <code>AND</code> connected.</p>
          <v-row dense>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData.id"
                clearable
                label="ID" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-if="!hideFields.hideNameField"
                v-model="advancedSearchData.name"
                clearable
                label="Name" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-if="!hideFields.hideInternalNameField"
                v-model="advancedSearchData.internal_name"
                clearable
                label="Internal Name" />
            </v-col>
          </v-row>
          <v-row v-if="!loadingFields && renderedFields" dense>
            <v-col v-for="field in renderedFields" :key="`f-${field.attr_name}`" cols="3">
              <v-select
                v-if="field.type === 'boolean'"
                v-model="advancedSearchData[field.attr_name]"
                clearable
                :items="booleanItems"
                item-text="name"
                item-value="value"
                :label="field.attr_friendly_name" />
              <v-text-field
                v-if="(field.type === 'keyword' && field.attr_name !== 'column_type') || field.type === 'text' || field.type === 'date'"
                v-model="advancedSearchData[field.attr_name]"
                type="text"
                :label="field.attr_friendly_name"
                clearable />
              <v-select
                v-if="field.type === 'keyword' && field.attr_name === 'column_type'"
                v-model="advancedSearchData[field.attr_name]"
                :items="columnTypes"
                item-value="value"
                clearable
                :label="field.attr_friendly_name" />
              <v-text-field
                v-if="field.type.startsWith('integer') || field.type.startsWith('long') || field.type.startsWith('double')"
                v-model="advancedSearchData[field.attr_name]"
                type="number"
                :label="field.attr_friendly_name"
                clearable />
              <v-autocomplete
                v-if="field.attr_name === 'licenses'"
                v-model="advancedSearchData[field.attr_name]"
                :items="fetchLicenses"
                :label="field.attr_friendly_name"
                clearable
                multiple />
            </v-col>
          </v-row>
          <p v-if="isEligibleYearRangeSearch" class="mt-4">
            Specify your custom publication year range:
          </p>
          <v-row v-if="isEligibleYearRangeSearch" dense>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData['t1']"
                type="number"
                label="Start Year"
                required
                :rules="[v => !!v || $t('Required')]"
                clearable />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData['t2']"
                type="number"
                label="End Year"
                clearable />
            </v-col>
          </v-row>
          <p v-if="isEligibleUnitIndependentSearch" class="mt-4">
            If you select a <code>concept</code> and <code>unit</code>, you can search across columns regardless of their
            unit of measurement.
          </p>
          <v-row v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch" dense>
            <v-col v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch" cols="3">
              <v-select
                v-model="advancedSearchData['tables.columns.concept.uri']"
                clearable
                :items="concepts"
                item-text="name"
                item-value="uri"
                label="Concept" />
            </v-col>
            <v-col v-if="isEligibleConceptOrUnitSearch || isEligibleUnitIndependentSearch" cols="3">
              <v-select
                v-model="advancedSearchData['tables.columns.unit.uri']"
                clearable
                :items="units"
                item-text="name"
                item-value="uri"
                label="Unit" />
            </v-col>
            <v-col v-if="isEligibleUnitIndependentSearch" cols="3">
              <v-text-field
                v-model="advancedSearchData['t1']"
                clearable
                type="number"
                label="Start Value" />
            </v-col>
            <v-col v-if="isEligibleUnitIndependentSearch" cols="3">
              <v-text-field
                v-model="advancedSearchData['t2']"
                clearable
                type="number"
                label="End Value" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-btn
              type="submit"
              class="mr-2"
              color="primary"
              :loading="loading"
              :disabled="!valid"
              small
              @click="advancedSearch">
              Search
            </v-btn>
          </v-row>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>
<script>
import SearchService from '@/api/search.service'
import QueryMapper from '@/api/query.mapper'
import SemanticService from '@/api/semantic.service'
import SemanticMapper from '@/api/semantic.mapper'

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
      columnTypes: QueryMapper.mySql8DataTypes().map((datatype) => {
        datatype.value = datatype.value.toUpperCase()
        return datatype
      }),
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
    SemanticService.findAllConcepts()
      .then((response) => {
        this.concepts = SemanticMapper.mapConcepts(response)
      })
    SemanticService.findAllUnits()
      .then((response) => {
        this.units = SemanticMapper.mapUnits(response)
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
      SearchService.search(this.searchType, this.advancedSearchData)
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
      SearchService.getFields(searchType)
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
