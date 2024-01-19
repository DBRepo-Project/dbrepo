<template>
  <div id="persist">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="backTo">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title v-text="pageTitle" />
      <v-spacer />
      <v-toolbar-title>
        <v-btn
          v-if="!isUpdate"
          class="mb-1"
          :loading="loading"
          :disabled="!formValid || loading"
          color="primary"
          @click="save">
          <v-icon left>mdi-content-save-outline</v-icon> Create PID
        </v-btn>
        <v-btn
          v-if="isUpdate"
          class="mb-1"
          :loading="loading"
          :disabled="!formValid || loading"
          color="primary"
          @click="save">
          <v-icon left>mdi-content-save-outline</v-icon> Update PID
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-form ref="form" v-model="formValid">
      <v-card tile elevation="0">
        <v-card-title>Creators</v-card-title>
        <v-stepper v-for="(creator, i) in identifier.creators" :key="`c-${i}`" tile elevation="0" vertical>
          <v-stepper-step :step="i+1" class="pt-0 pb-0">
            <v-card-text class="pt-0 pb-0">
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="creator.name_identifier"
                    label="Name Identifier"
                    clearable
                    name="name-identifier"
                    hint="Use a name identifier expressed as URL from ORCID*, ROR*, DOI*, ISNI, GND (schemes with * support automatic metadata retrieval)"
                    :loading="creator.name_loading"
                    persistent-hint
                    required
                    @focusout="retrieveCreator(creator)" />
                </v-col>
                <v-col cols="4" class="mt-5">
                  <v-btn :disabled="!canShiftUp(creator, i)" small @click="shiftUp(i)">
                    <v-icon small>mdi-arrow-up</v-icon>
                  </v-btn>
                  <v-btn :disabled="!canShiftDown(creator, i)" small @click="shiftDown(i)">
                    <v-icon small>mdi-arrow-down</v-icon>
                  </v-btn>
                  <v-btn v-if="canInsertSelf" color="secondary" small @click="insertSelf(creator)">
                    Insert Myself
                  </v-btn>
                  <v-btn v-if="i > 0" color="error" small @click="deleteCreator(i)">
                    Remove
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-step>
          <v-stepper-content :step="1">
            <v-card-text>
              <v-row dense>
                <v-col cols="8">
                  <v-radio-group v-model="creator.name_type" row>
                    <v-radio
                      label="Person"
                      value="Personal" />
                    <v-radio
                      label="Organization"
                      value="Organizational" />
                  </v-radio-group>
                </v-col>
              </v-row>
              <v-row
                v-if="isPerson(creator)"
                dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="creator.firstname"
                    label="Given Name"
                    clearable
                    hint="e.g. John"
                    required
                    @focusout="suggestName(creator)" />
                </v-col>
              </v-row>
              <v-row
                v-if="isPerson(creator)"
                dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="creator.lastname"
                    label="Family Name"
                    clearable
                    hint="e.g. Doe"
                    required
                    @focusout="suggestName(creator)" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="creator.creator_name"
                    label="Name *"
                    hint="e.g. Doe, Joe"
                    :rules="[v => !!v || $t('Required')]"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="creator.affiliation_identifier"
                    label="Affiliation Identifier"
                    name="affiliation-identifier"
                    :loading="creator.affiliation_loading"
                    hint="Use an affiliation identifier expressed as URL from ORCID*, ROR*, DOI*, ISNI, GND (schemes with * support automatic metadata retrieval)"
                    persistent-hint
                    clearable
                    @focusout="retrieveAffiliation(creator)" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="creator.affiliation"
                    label="Affiliation"
                    name="affiliation"
                    clearable
                    hint="e.g. Brown University" />
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-content>
        </v-stepper>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addCreator">
                Add Creator
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Titles</v-card-title>
        <v-stepper v-for="(title, i) in identifier.titles" :key="`t-${i}`" tile elevation="0" vertical>
          <v-stepper-step :step="i+1" class="pt-0 pb-0">
            <v-card-text>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="title.title"
                    label="Title *"
                    :rules="[v => !!v || $t('Required')]"
                    required />
                </v-col>
                <v-col v-if="i > 0" cols="2" class="mt-5">
                  <v-btn color="error" small @click="deleteTitle(i)">
                    Remove
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-step>
          <v-stepper-content :step="1">
            <v-card-text>
              <v-row dense>
                <v-col cols="8">
                  <v-select
                    v-model="title.type"
                    label="Type"
                    clearable
                    :items="titleType"
                    item-text="value"
                    item-value="value"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-autocomplete
                    v-model="title.language"
                    label="Language"
                    clearable
                    :items="languages"
                    item-text="name"
                    item-value="code"
                    required />
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-content>
        </v-stepper>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addTitle">
                Add Title
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Descriptions</v-card-title>
        <v-stepper v-for="(description, i) in identifier.descriptions" :key="`d-${i}`" tile elevation="0" vertical>
          <v-stepper-step :step="i+1" class="pt-0 pb-0">
            <v-card-text>
              <v-row dense>
                <v-col cols="8">
                  <v-textarea
                    v-model="description.description"
                    label="Description *"
                    :rules="[v => !!v || $t('Required')]"
                    rows="1" />
                </v-col>
                <v-col v-if="i > 0" cols="2" class="mt-5">
                  <v-btn color="error" small @click="deleteDescription(i)">
                    Remove
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-step>
          <v-stepper-content :step="1">
            <v-card-text>
              <v-row dense>
                <v-col cols="8">
                  <v-select
                    v-model="description.type"
                    label="Type"
                    clearable
                    :items="descriptionType"
                    item-text="value"
                    item-value="value"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-autocomplete
                    v-model="description.language"
                    label="Language"
                    clearable
                    :items="languages"
                    item-text="name"
                    item-value="code"
                    required />
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-content>
        </v-stepper>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addDescription">
                Add Description
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Publication</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                id="publisher"
                v-model="identifier.publisher"
                name="publisher"
                :label="`${prefix} publisher *`"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="2">
              <v-text-field
                id="publication-day"
                v-model.number="identifier.publication_day"
                type="number"
                label="Publication day"
                clearable />
            </v-col>
            <v-col cols="2">
              <v-text-field
                id="publication-month"
                v-model.number="identifier.publication_month"
                type="number"
                label="Publication month"
                clearable />
            </v-col>
            <v-col cols="2">
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
        <v-card-title>Related Identifiers</v-card-title>
        <v-stepper v-for="(related, i) in identifier.related_identifiers" :key="`r-${i}`" tile elevation="0" vertical>
          <v-stepper-step :step="i+1" class="pt-0 pb-0">
            <v-card-text>
              <v-row dense>
                <v-col cols="4">
                  <v-text-field
                    v-model="related.value"
                    name="related"
                    label="Identifier *"
                    :rules="[v => !!v || $t('Required')]"
                    required />
                </v-col>
                <v-col cols="2">
                  <v-select
                    v-model="related.type"
                    :items="relatedTypes"
                    item-value="value"
                    item-text="value"
                    label="Type"
                    clearable />
                </v-col>
                <v-col cols="2">
                  <v-select
                    v-model="related.relation"
                    :items="relationTypes"
                    item-value="value"
                    item-text="value"
                    label="Relation"
                    clearable />
                </v-col>
                <v-col cols="2" class="mt-5">
                  <v-btn color="error" small @click="deleteRelatedIdentifier(i)">
                    Remove
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-step>
        </v-stepper>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addRelatedIdentifier">
                Add Related Identifier
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title v-if="isDatabase">Licenses</v-card-title>
        <v-card-text v-if="isDatabase">
          <v-row dense>
            <v-col cols="8">
              <v-alert
                v-if="identifier.licenses.length > 1"
                border="left"
                color="warning">
                <strong>We do not recommend selecting multiple licenses.</strong> If you are sure you need multiple licenses, specify in the description which denomination (e.g. table, subset, view) has which license.
              </v-alert>
              <v-select
                v-model="identifier.licenses"
                return-object
                :items="licenses"
                multiple
                item-text="identifier"
                label="Licenses *"
                :rules="[ v => !!v || $t('Required') ]"
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Language</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col cols="8">
              <v-autocomplete
                v-model="identifier.language"
                label="Language"
                clearable
                :items="languages"
                item-text="name"
                item-value="code"
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-title>Funding Information</v-card-title>
        <v-stepper v-for="(funder, i) in identifier.funders" :key="`f-${i}`" tile elevation="0" vertical>
          <v-stepper-step :step="i+1" class="pt-0 pb-0">
            <v-card-text class="pt-0 pb-0">
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="funder.funder_identifier"
                    label="Funder Identifier"
                    name="funder-identifier"
                    hint="Use a name identifier expressed as URL from ORCID*, ROR*, DOI*, ISNI, GND (schemes with * support automatic metadata retrieval)"
                    :loading="funder.loading"
                    persistent-hint
                    required
                    clearable
                    @focusout="retrieveFunder(funder)" />
                </v-col>
                <v-col cols="4" class="mt-5">
                  <v-btn color="error" small @click="deleteFunder(i)">
                    Remove
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-step>
          <v-stepper-content :step="1">
            <v-card-text>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="funder.funder_name"
                    label="Name *"
                    hint="e.g. European Commission"
                    :rules="[v => !!v || $t('Required')]"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="funder.award_number"
                    label="Award Number"
                    clearable
                    hint="e.g. CBET-106" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="8">
                  <v-text-field
                    v-model="funder.award_title"
                    label="Award Title"
                    clearable />
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-content>
        </v-stepper>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addFunding">
                Add Funding
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { formatYearUTC, formatMonthUTC, formatDayUTC, languages } from '@/utils'
import IdentifierService from '@/api/identifier.service'
import DatabaseService from '@/api/database.service'
import UserMapper from '@/api/user.mapper'
import identifierMapper from '@/api/identifier.mapper'

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
    },
    query: {
      type: Object,
      default () {
        return {}
      }
    },
    view: {
      type: Object,
      default () {
        return {}
      }
    },
    table: {
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
      identifier: {
        database_id: parseInt(this.$route.params.database_id),
        query_id: parseInt(this.$route.params.query_id),
        view_id: parseInt(this.$route.params.view_id),
        table_id: parseInt(this.$route.params.table_id),
        titles: [],
        descriptions: [],
        publisher: this.$config.defaultPublisher,
        publication_year: formatYearUTC(Date.now()),
        publication_month: formatMonthUTC(Date.now()),
        publication_day: formatDayUTC(Date.now()),
        licenses: [],
        type: this.type,
        creators: [],
        related_identifiers: [],
        funders: []
      },
      titleType: [
        { value: 'AlternativeTitle' },
        { value: 'Subtitle' },
        { value: 'TranslatedTitle' },
        { value: 'Other' }
      ],
      descriptionType: [
        { value: 'Abstract' },
        { value: 'Methods' },
        { value: 'SeriesInformation' },
        { value: 'TableOfContents' },
        { value: 'TechnicalInfo' },
        { value: 'Other' }
      ],
      languages: languages(),
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
    user () {
      return this.$store.state.user
    },
    isSubset () {
      return this.type === 'subset'
    },
    isDatabase () {
      return this.type === 'database'
    },
    isView () {
      return this.type === 'view'
    },
    isTable () {
      return this.type === 'table'
    },
    backTo () {
      if (this.isSubset) {
        return `/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`
      } else if (this.isDatabase) {
        return `/database/${this.$route.params.database_id}`
      } else if (this.isView) {
        return `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
      } else if (this.isTable) {
        return `/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`
      }
      return null
    },
    pageTitle () {
      return (this.isUpdate ? 'Update' : 'Create') + ' Identifier'
    },
    isUpdate () {
      return 'id' in this.identifier && this.identifier.id
    },
    canInsertSelf () {
      if (!this.user.attributes || !this.user.attributes.orcid) {
        return false
      }
      return this.identifier.creators.filter(c => c.name_identifier === this.user.attributes.orcid).length === 0
    },
    prefix () {
      if (this.isSubset) {
        return 'Subset'
      } else if (this.isDatabase) {
        return 'Database'
      } else if (this.isView) {
        return 'View'
      } else if (this.isTable) {
        return 'Table'
      }
      return ''
    }
  },
  watch: {
    database () {
      this.init()
    },
    query () {
      this.init()
    },
    view () {
      this.init()
    }
  },
  mounted () {
    this.addCreator()
    this.addTitle()
    this.addDescription()
    this.loadLicenses()
    this.init()
  },
  methods: {
    cancel () {
      this.$emit('close', { action: 'closed' })
    },
    retrieveCreator (creator) {
      if (!creator || !creator.name_identifier) {
        creator.name_identifier_scheme = null
        return
      }
      creator.name_loading = true
      IdentifierService.retrieve(creator.name_identifier)
        .then((metadata) => {
          creator.success = true
          creator.firstname = metadata.given_names
          creator.lastname = metadata.family_name
          creator.name_type = metadata.type
          if (metadata.type === 'Organizational' && metadata.affiliations) {
            creator.creator_name = metadata.affiliations[0].organization_name
            creator.affiliation = null
          } else {
            creator.creator_name = (creator.lastname + ', ' + creator.firstname)
            if (metadata.affiliations.length > 0) {
              creator.affiliation = metadata.affiliations[0].organization_name
            }
          }
          creator.name_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(creator.name_identifier)
        })
        .catch(() => {
          creator.success = false
        })
        .finally(() => {
          creator.name_loading = false
        })
    },
    suggestName (creator) {
      if (!creator.firstname || !creator.lastname) {
        return
      }
      creator.creator_name = creator.lastname + ', ' + creator.firstname
    },
    isPerson (creator) {
      if (!creator || !creator.name_type) {
        return false
      }
      return creator.name_type === 'Personal'
    },
    retrieveAffiliation (creator) {
      if (!creator || !creator.affiliation_identifier) {
        creator.affiliation_identifier_scheme = null
        return
      }
      creator.affiliation_loading = true
      IdentifierService.retrieve(creator.affiliation_identifier)
        .then((metadata) => {
          creator.success = true
          if (creator.type === 'Organizational') {
            creator.creator_name = metadata.affiliations[0].organization_name
          } else {
            creator.affiliation = metadata.affiliations[0].organization_name
          }
          creator.affiliation_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(creator.affiliation_identifier)
        })
        .catch(() => {
          creator.success = false
        })
        .finally(() => {
          creator.affiliation_loading = false
        })
    },
    retrieveFunder (funder) {
      if (!funder || !funder.funder_identifier) {
        funder.funder_identifier_scheme = null
        return
      }
      funder.loading = true
      IdentifierService.retrieve(funder.funder_identifier)
        .then((metadata) => {
          if (metadata.type === 'Organizational' && metadata.affiliations) {
            funder.funder_name = metadata.affiliations[0].organization_name
          }
          funder.funder_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(funder.name_identifier)
        })
        .catch(() => {
          funder.success = false
        })
        .finally(() => {
          funder.loading = false
        })
    },
    addCreator () {
      this.identifier.creators.push({
        firstname: null,
        lastname: null,
        affiliation: null,
        affiliation_identifier: null,
        affiliation_identifier_scheme: null,
        name_identifier: null,
        name_identifier_scheme: null,
        name_type: 'Personal',
        creator_name: null,
        name_loading: false /* removed later */,
        affiliation_loading: false /* removed later */,
        success: false /* removed later */
      })
    },
    addTitle () {
      this.identifier.titles.push({
        title: null,
        type: null,
        language: null
      })
    },
    addFunding () {
      this.identifier.funders.push({
        funder_name: null,
        funder_identifier: null,
        funder_identifier_type: null,
        award_number: null,
        award_title: null,
        loading: false /* removed later */,
        success: false /* removed later */
      })
    },
    addDescription () {
      this.identifier.descriptions.push({
        description: null,
        type: null,
        language: null
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
    deleteFunder (index) {
      this.identifier.funders.splice(index, 1)
    },
    deleteTitle (index) {
      if (index === 0) {
        return
      }
      this.identifier.titles.splice(index, 1)
    },
    deleteDescription (index) {
      if (index === 0) {
        return
      }
      this.identifier.descriptions.splice(index, 1)
    },
    deleteRelatedIdentifier (index) {
      this.identifier.related_identifiers.splice(index, 1)
    },
    save () {
      this.loading = true
      const payload = identifierMapper.identifierToIdentifierSave(this.identifier)
      if (this.isUpdate) {
        IdentifierService.update(this.identifier.id, payload)
          .then(() => {
            console.info('Updated identifier with id', this.identifier.id)
            this.$store.dispatch('reloadDatabase')
            this.$router.push(this.backTo)
            this.$toast.success(this.prefix + ' successfully persisted')
          })
          .catch(() => {
            this.loading = false
          })
          .finally(() => {
            this.loading = false
          })
      } else {
        IdentifierService.create(payload)
          .then(async () => {
            console.info('Created identifier')
            await this.$store.dispatch('reloadDatabase')
            await this.$toast.success(this.prefix + ' successfully persisted')
            await this.$router.push(this.backTo)
          })
          .catch(() => {
            this.loading = false
          })
          .finally(() => {
            this.loading = false
          })
      }
    },
    loadLicenses () {
      this.loading = true
      DatabaseService.findAllLicenses()
        .then((licenses) => {
          this.licenses = licenses
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    init () {
      if (this.isDatabase && this.database && 'identifier' in this.database && this.database.identifier) {
        this.identifier = Object.assign(this.database.identifier, {})
      } else if (this.isSubset && this.query && 'identifier' in this.query && this.query.identifier) {
        this.identifier = Object.assign(this.query.identifier, {})
      } else if (this.isView && this.view && 'identifier' in this.view && this.view.identifier) {
        this.identifier = Object.assign(this.view.identifier, {})
      }
    },
    insertSelf (creator) {
      creator.name_identifier = this.user.attributes.orcid
      this.retrieveCreator(creator)
    },
    canShiftUp (creator, idx) {
      if (this.identifier.creators.length === 1 || idx === 0) {
        return false
      }
      return true
    },
    canShiftDown (creator, idx) {
      if (this.identifier.creators.length === 1 || idx + 1 === this.identifier.creators.length) {
        return false
      }
      return true
    },
    shiftUp (idx) {
      this.arrayMove(this.identifier.creators, idx, idx - 1)
    },
    shiftDown (idx) {
      this.arrayMove(this.identifier.creators, idx, idx + 1)
    },
    arrayMove (array, fromIndex, toIndex) {
      const element = array[fromIndex]
      array.splice(fromIndex, 1)
      array.splice(toIndex, 0, element)
    }
  }
}
</script>
<style>
#persist .v-stepper .v-stepper__step--active .v-stepper__label {
  text-shadow: none !important;
}
</style>
