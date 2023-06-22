<template>
  <div>
    <v-form ref="form" v-model="formValid" autocomplete="off">
      <v-card>
        <v-card-title>Creators</v-card-title>
        <v-card-text v-for="(creator, i) in identifier.creators" :key="`c-${i}`">
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="creator.identifier"
                label="Name Identifier"
                hint="Use a name identifier expressed as URL for automatic metadata retrieval of schemas: ORCID or ROR"
                :loading="creator.loading"
                persistent-hint
                required
                @focusout="retrieve(creator)" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-radio-group v-model="creator.type">
                <v-radio
                  label="Person"
                  value="person" />
                <v-radio
                  label="Organization"
                  value="organization" />
              </v-radio-group>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="creator.firstname"
                label="Given Name"
                :disabled="creator.success || creator.type === 'organization'"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="creator.lastname"
                label="Family Name"
                :disabled="creator.success || creator.type === 'organization'"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="creator.name"
                label="Name *"
                :disabled="creator.success && creator.type === 'organization'"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col v-if="i > 0" cols="1" class="mt-5">
              <v-btn icon x-small @click="deleteCreator(i)">
                <v-icon>mdi-close</v-icon>
              </v-btn>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addCreator">
                Add Creator
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Titles</v-card-title>
        <v-card-text v-for="(title, i) in identifier.titles" :key="`t-${i}`">
          <v-row dense>
            <v-col>
              <v-text-field
                v-model="title.name"
                autofocus
                label="Title"
                placeholder="Type ..."
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                v-model="title.language"
                autofocus
                label="Language"
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small>
                Add Title
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Descriptions</v-card-title>
        <v-card-text v-for="(description, j) in identifier.descriptions" :key="`d-${j}`">
          <v-row dense>
            <v-col>
              <v-textarea
                v-model="description.text"
                label="Text"
                rows="2" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                v-model="description.type"
                autofocus
                label="Type"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                v-model="description.language"
                autofocus
                label="Language"
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small>
                Add Description
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-row v-if="isSubset" dense>
            <v-col>
              <v-select
                v-model="identifier.visibility"
                :items="visibilities"
                item-text="name"
                item-value="value"
                label="Visibility *"
                :hint="visibilityHint"
                persistent-hint
                :rules="[ v => !!v || $t('Required') ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="publisher"
                v-model="identifier.publisher"
                name="publisher"
                :label="`${prefix} publisher *`"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="2">
              <v-text-field
                id="publication-day"
                v-model.number="identifier.publication_day"
                type="number"
                label="Publication day" />
            </v-col>
            <v-col cols="2">
              <v-text-field
                id="publication-month"
                v-model.number="identifier.publication_month"
                type="number"
                label="Publication month" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                id="publication-year"
                v-model.number="identifier.publication_year"
                type="number"
                label="Publication year *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-row v-for="(related,i) in identifier.related_identifiers" :key="`r-${i}`" dense>
            <v-col cols="4">
              <v-text-field
                v-model="related.value"
                name="related"
                label="Identifier *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col cols="3">
              <v-select
                v-model="related.type"
                :items="relatedTypes"
                item-value="value"
                item-text="value"
                label="Type" />
            </v-col>
            <v-col cols="3">
              <v-select
                v-model="related.relation"
                :items="relationTypes"
                item-value="value"
                item-text="value"
                label="Relation" />
            </v-col>
            <v-col cols="1" class="mt-5">
              <v-btn color="error" icon x-small @click="deleteRelatedIdentifier(i)">
                <v-icon>mdi-delete</v-icon>
              </v-btn>
            </v-col>
          </v-row>
          <v-row v-if="isDatabase" dense>
            <v-col>
              <v-select
                v-model="identifier.license"
                return-object
                :items="licenses"
                item-text="identifier"
                label="License *"
                :rules="[ v => !!v || $t('Required') ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addRelatedIdentifier">
                Add Related Identifier
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            v-if="isUpdate"
            class="mb-2"
            :loading="loading"
            :disabled="!formValid || loading"
            color="primary"
            @click="update">
            Update
          </v-btn>
          <v-btn
            v-if="!isUpdate"
            class="mb-2"
            :loading="loading"
            :disabled="!formValid || loading"
            color="primary"
            @click="persist">
            Persist
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { formatYearUTC, formatMonthUTC, formatDayUTC } from '@/utils'
import IdentifierService from '@/api/identifier.service'
import DatabaseService from '@/api/database.service'

export default {
  props: {
    type: {
      type: String,
      default: 'subset'
    },
    database: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      formValid: false,
      loading: false,
      error: false, // XXX: `error` is never changed
      licenses: [],
      visibilities: [
        { name: 'Public', value: 'everyone' },
        { name: 'Private', value: 'self' }
      ],
      identifier: {
        dbid: parseInt(this.$route.params.database_id),
        qid: parseInt(this.$route.params.query_id),
        titles: [
          {
            name: null,
            language: null
          }
        ],
        descriptions: [
          {
            text: null,
            type: null,
            language: null
          }
        ],
        visibility: 'everyone',
        publisher: this.$config.defaultPublisher,
        publication_year: formatYearUTC(Date.now()),
        publication_month: formatMonthUTC(Date.now()),
        publication_day: formatDayUTC(Date.now()),
        license: null,
        type: this.type,
        creators: [
          {
            firstname: null,
            lastname: null,
            affiliation: null,
            orcid: null,
            type: 'person',
            name: null,
            identifier: null,
            loading: false,
            success: false
          }
        ],
        related_identifiers: []
      },
      relatedTypes: [
        { value: 'DOI' },
        { value: 'URL' },
        { value: 'URN' },
        { value: 'ARK' },
        { value: 'arXiv' },
        { value: 'bibcode' },
        { value: 'EAN13' },
        { value: 'EISSN' },
        { value: 'Handle' },
        { value: 'IGSN' },
        { value: 'ISBN' },
        { value: 'ISTC' },
        { value: 'LISSN' },
        { value: 'LSID' },
        { value: 'PMID' },
        { value: 'PURL' },
        { value: 'UPC' },
        { value: 'w3id' }
      ],
      relationTypes: [
        { value: 'IsCitedBy' },
        { value: 'Cites' },
        { value: 'IsSupplementTo' },
        { value: 'IsSupplementedBy' },
        { value: 'IsContinuedBy' },
        { value: 'Continues' },
        { value: 'IsDescribedBy' },
        { value: 'Describes' },
        { value: 'HasMetadata' },
        { value: 'IsMetadataFor' },
        { value: 'HasVersion' },
        { value: 'IsVersionOf' },
        { value: 'IsNewVersionOf' },
        { value: 'IsPreviousVersionOf' },
        { value: 'IsPartOf' },
        { value: 'HasPart' },
        { value: 'IsPublishedIn' },
        { value: 'IsReferencedBy' },
        { value: 'References' },
        { value: 'IsDocumentedBy' },
        { value: 'Documents' },
        { value: 'IsCompiledBy' },
        { value: 'Compiles' },
        { value: 'IsVariantFormOf' },
        { value: 'IsOriginalFormOf' },
        { value: 'IsIdenticalTo' },
        { value: 'IsReviewedBy' },
        { value: 'Reviews' },
        { value: 'IsDerivedFrom' },
        { value: 'IsSourceOf' },
        { value: 'IsRequiredBy' },
        { value: 'Requires' },
        { value: 'IsObsoletedBy' },
        { value: 'Obsoletes' }
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
    user () {
      return this.$store.state.user
    },
    isSubset () {
      return this.type === 'subset'
    },
    isDatabase () {
      return this.type === 'database'
    },
    isUpdate () {
      if (!this.database.identifier) {
        return false
      }
      return this.database.identifier.id !== null
    },
    visibilityHint () {
      if (this.identifier.visibility === 'public') {
        return 'The result set will be open access (world-readable)'
      }
      return 'The result set will be visible only to you'
    },
    prefix () {
      if (this.isSubset) {
        return 'Subset'
      } else if (this.isDatabase) {
        return 'Database'
      }
      return ''
    }
  },
  watch: {
    database () {
      if (!this.database.identifier) {
        return
      }
      this.identifier = Object.assign(this.database.identifier, {})
    }
  },
  mounted () {
    this.loadLicenses()
    if (this.database.identifier) {
      this.identifier = Object.assign(this.database.identifier, {})
    }
  },
  methods: {
    cancel () {
      this.$emit('close', { action: 'closed' })
    },
    retrieve (creator) {
      creator.loading = true
      IdentifierService.retrieve(creator.identifier)
        .then((metadata) => {
          creator.success = true
          creator.firstname = metadata.given_names
          creator.lastname = metadata.family_name
          creator.type = metadata.type
          if (metadata.type === 'organization' && metadata.affiliations) {
            creator.name = metadata.affiliations[0].organization_name
          }
        })
        .catch(() => {
          creator.success = false
        })
        .finally(() => {
          creator.loading = false
        })
    },
    addCreator () {
      this.identifier.creators.push({
        firstname: null,
        lastname: null,
        affiliation: null,
        orcid: null
      })
    },
    addRelatedIdentifier () {
      this.identifier.related_identifiers.push({
        value: null,
        relation: 'Cites',
        type: 'DOI'
      })
    },
    deleteCreator (index) {
      if (index === 0) {
        return
      }
      this.identifier.creators.splice(index, 1)
    },
    deleteRelatedIdentifier (index) {
      this.identifier.related_identifiers.splice(index, 1)
    },
    persist () {
      this.loading = true
      IdentifierService.create(this.identifier)
        .then(() => {
          this.$toast.success(this.prefix + ' successfully persisted')
          this.$emit('close', { action: 'persisted' })
        })
        .finally(() => {
          this.loading = false
        })
    },
    update () {
      this.loading = true
      const payload = {
        dbid: parseInt(this.$route.params.database_id),
        qid: parseInt(this.$route.params.query_id),
        title: this.identifier.title,
        description: this.identifier.description,
        publisher: this.identifier.publisher,
        publication_year: this.identifier.publication_year,
        publication_month: this.identifier.publication_month,
        publication_day: this.identifier.publication_day,
        license: this.identifier.license,
        type: this.identifier.type,
        creators: this.identifier.creators,
        related_identifiers: this.identifier.related_identifiers
      }
      IdentifierService.update(this.identifier.id, payload)
        .then(() => {
          this.$toast.success(this.prefix + ' identifier successfully updated')
          this.$emit('close', { action: 'persisted' })
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadLicenses () {
      if (!this.token) {
        return
      }
      this.loading = true
      DatabaseService.findAllLicenses()
        .then((licenses) => {
          this.licenses = licenses
        })
        .finally(() => {
          this.loading = false
        })
    },
    validateOrcidInput (val) {
      if (!val) {
        return false
      }
      return val.startsWith('http')
    }
  }
}
</script>
<style>
#creators,
#creators-btn {
  background-color: #f00;
  margin-left: -16px !important;
  margin-right: -16px !important;
}
</style>
