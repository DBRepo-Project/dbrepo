<template>
  <div>
    <v-card v-if="isAdvancedSearch" flat tile>
      <v-card-text class="pt-0 pl-4 pb-6 pr-4">
        <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="3">
              <v-select
                v-model="index"
                :items="fieldItems"
                item-text="name"
                item-value="value"
                solo
                label="Type" />
            </v-col>
          </v-row>
          <p>The following fields are <code>AND</code> connected and depend on the type above.</p>
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
          <v-row v-if="loadingFields" dense>
            <v-progress-circular color="primary" indeterminate />
          </v-row>
          <v-row v-if="!loadingFields && renderedFields" dense>
            <v-col v-for="field in renderedFields" :key="`f-${field.attribute_name}`" cols="3">
              <v-select
                v-if="field.type === 'boolean'"
                v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                clearable
                :items="booleanItems"
                item-text="name"
                item-value="value"
                :label="generateFriendlyName(field)" />
              <v-text-field
                v-if="(field.type === 'keyword' && field.attribute_name !== 'column_type') || field.type === 'text' || field.type === 'date'"
                v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                type="text"
                :label="generateFriendlyName(field)"
                clearable />
              <v-select
                v-if="field.type === 'keyword' && field.attribute_name === 'column_type'"
                v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                :items="columnTypes"
                item-value="value"
                clearable
                :label="generateFriendlyName(field)" />
              <v-text-field
                v-if="field.type === 'integer'"
                v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                type="number"
                :label="generateFriendlyName(field)"
                clearable />
              <v-autocomplete
                v-if="field.attribute_name === 'licenses'"
                v-model="advancedSearchData[generateDynamicVModelKey(field)]"
                :items="fetchLicenses()"
                :label="generateFriendlyName(field)"
                clearable
                multiple />
            </v-col>
          </v-row>
          <p v-if="isEligibleConceptOrUnitSearch" class="mt-4">
            If you select a <code>concept</code> and <code>unit</code>, you can search across columns regardless of their
            unit of measurement.
          </p>
          <v-row v-if="isEligibleConceptOrUnitSearch" dense>
            <v-col cols="3">
              <v-select
                v-model="advancedSearchData['concept.uri']"
                clearable
                :items="concepts"
                item-text="name"
                item-value="uri"
                label="Concept" />
            </v-col>
            <v-col cols="3">
              <v-select
                v-model="advancedSearchData['unit.uri']"
                clearable
                :items="units"
                item-text="name"
                item-value="uri"
                label="Unit" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="advancedSearchData['t1']"
                clearable
                type="number"
                label="Start Value" />
            </v-col>
            <v-col cols="3">
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
      index: 'database',
      valid: false,
      loading: false,
      loadingFields: false,
      showAdvancedSearch: false,
      concepts: [],
      units: [],
      columnTypes: QueryMapper.mySql8DataTypes().map((datatype) => {
        datatype.value = datatype.value.toUpperCase()
        return datatype
      }),
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
      const selectedOption = this.index
      return {
        hideNameField: selectedOption === 'identifier',
        hideInternalNameField: ['identifier', 'user', 'concept', 'unit'].includes(selectedOption)
      }
    },
    isEligibleConceptOrUnitSearch () {
      return ['column'].includes(this.index)
    },
    isAdvancedSearch () {
      return !this.$route.query.q
    }
  },
  watch: {
    index: {
      handler (newType, oldType) {
        if (!newType) {
          return
        }
        this.resetAdvancedSearchFields()
        this.$emit('search-result', [])
        this.loadingFields = true
        SearchService.getFields(newType)
          .then((response) => {
            this.loadingFields = false
            const { fields } = response
            this.renderedFields = fields.filter(field => this.shouldRenderItem(field))
          })
          .finally(() => {
            this.loadingFields = false
          })
      },
      immediate: true
    }
  },
  mounted () {
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
        .filter(k => !['name', 'internal_name', 'id', 'type'].includes(k))
        .forEach(k => delete this.advancedSearchData[k])
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
      SearchService.search(this.index, this.advancedSearchData)
        .then((response) => {
          this.$emit('search-result', response.map(h => h._source))
        })
        .finally(() => {
          this.loading = false
        })
    },
    isAdvancedSearchEmpty () {
      return !(
        this.advancedSearchData.id ||
        this.advancedSearchData.name ||
        this.advancedSearchData.internal_name
      )
    },
    dynamicFieldsMap () {
      // Defines a mapping to narrow down the fields rendered for the advanced search
      return {
        database: ['is_public'],
        table: ['description', 'is_public'],
        column: ['column_type', 'is_primary_key', 'is_null_allowed'],
        user: ['firstname', 'lastname', 'username', 'attributes.properties.orcid'],
        identifier: [
          'creators.properties.creator_name', 'creators.properties.name_identifier',
          'descriptions.properties.description', 'doi', 'funders.properties.funder_identifier',
          'publication_year', 'titles.properties.title'
        ],
        view: ['is_public', 'query'],
        concept: ['uri'],
        unit: ['uri']
      }
    },
    getLastFlattenedItem (str) {
      // Returns substring after the last dot otherwise the string itself if no dots are contained
      if (!str) { return '' }

      // Check if string is a flattened nested object
      return str.includes('.') ? str.split('.').slice(-1)[0] : str
    },
    generateFriendlyName (item) {
      // Generates a proper name to be displayed with the dynamic component
      if (!item) { return '' }

      const specialAbbreviations = {
        doi: 'DOI',
        uri: 'URI'
        // Add more abbreviations here, if needed
      }
      const str = this.getLastFlattenedItem(item.attribute_name)

      return str.split('_').map((word) => {
        const lowerWord = word.toLowerCase()
        return specialAbbreviations[lowerWord] || (word.charAt(0).toUpperCase() + word.slice(1))
      }).join(' ')
    },
    generateDynamicVModelKey (item) {
      // Generates a dynamic v-model; It will be attached to the advancedSearchData object
      if (!item) { return '' }

      return `${this.index}.${item.attribute_name}`
    },
    shouldRenderItem (item) {
      // Checks if item's attribute_name matches any wanted field
      // The expected response is of a flattened format, so this method must be modified accordingly if the response is changed
      return this.dynamicFieldsMap()[this.index].includes(item.attribute_name)
    },
    fetchLicenses () {
      // Licenses is a nested object in the backend, but without any values.
      // Instead, we define our custom license generator with a controlled vocabulary.
      return [
        'Apache-2.0', 'BSD-3-Clause', 'BSD-4-Clause', 'CC-BY-4.0', 'CC0-1.0', 'GPL-3.0-only', 'MIT'
      ]
    }
  }
}
</script>
