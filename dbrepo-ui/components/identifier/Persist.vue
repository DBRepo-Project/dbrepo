<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="backTo">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>Create Identifier</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn
          v-if="!isUpdate"
          class="mb-1"
          :loading="loading"
          :disabled="!formValid || loading"
          color="primary"
          @click="persist">
          <v-icon left>mdi-identifier</v-icon> Persist
        </v-btn>
        <v-btn
          v-if="isUpdate"
          class="mb-1"
          :loading="loading"
          :disabled="!formValid || loading"
          color="primary"
          @click="update">
          <v-icon left>mdi-identifier</v-icon> Update
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-form ref="form" v-model="formValid" autocomplete="off">
      <v-card tile elevation="0">
        <v-card-title>Creators</v-card-title>
        <v-stepper v-for="(creator, i) in identifier.creators" :key="`c-${i}`" tile elevation="0" vertical>
          <v-stepper-step :step="i+1" class="pt-0 pb-0">
            <v-card-text class="pt-0 pb-0">
              <v-row dense>
                <v-col cols="10">
                  <v-text-field
                    v-model="creator.name_identifier"
                    label="Name Identifier"
                    :autofocus="i === 0"
                    hint="Use a name identifier expressed as URL for automatic metadata retrieval of schemas: ORCID or ROR"
                    :loading="creator.loading"
                    persistent-hint
                    required
                    clearable
                    @focusout="retrieve(creator)" />
                </v-col>
                <v-col v-if="i > 0" cols="2" class="mt-5">
                  <v-btn color="error" small @click="deleteCreator(i)">
                    Remove
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-stepper-step>
          <v-stepper-content :step="1">
            <v-card-text>
              <v-row dense>
                <v-col>
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
              <v-row dense>
                <v-col>
                  <v-text-field
                    v-model="creator.firstname"
                    label="Given Name"
                    hint="e.g. John"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col>
                  <v-text-field
                    v-model="creator.lastname"
                    label="Family Name"
                    hint="e.g. Doe"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col>
                  <v-text-field
                    v-model="creator.creator_name"
                    label="Name *"
                    hint="e.g. Doe, Joe"
                    :rules="[v => !!v || $t('Required')]"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col>
                  <v-text-field
                    v-model="creator.affiliation"
                    label="Affiliation"
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
                <v-col cols="10">
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
                <v-col>
                  <v-select
                    v-model="title.type"
                    label="Type"
                    :items="titleType"
                    item-text="value"
                    item-value="value"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col>
                  <v-autocomplete
                    v-model="title.language"
                    label="Language"
                    :items="languages"
                    item-text="value"
                    item-value="value"
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
                <v-col cols="10">
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
                <v-col>
                  <v-select
                    v-model="description.type"
                    label="Type"
                    :items="descriptionType"
                    item-text="value"
                    item-value="value"
                    required />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col>
                  <v-autocomplete
                    v-model="description.language"
                    label="Language"
                    :items="languages"
                    item-text="value"
                    item-value="value"
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
          <v-row dense>
            <v-col cols="4">
              <v-text-field
                id="publication-day"
                v-model.number="identifier.publication_day"
                type="number"
                label="Publication day" />
            </v-col>
            <v-col cols="4">
              <v-text-field
                id="publication-month"
                v-model.number="identifier.publication_month"
                type="number"
                label="Publication month" />
            </v-col>
            <v-col cols="4">
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
                <v-col cols="6">
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
                    label="Type" />
                </v-col>
                <v-col cols="2">
                  <v-select
                    v-model="related.relation"
                    :items="relationTypes"
                    item-value="value"
                    item-text="value"
                    label="Relation" />
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
        <v-card-title>License</v-card-title>
        <v-card-text>
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
        </v-card-text>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { formatYearUTC, formatMonthUTC, formatDayUTC } from '@/utils'
import IdentifierService from '@/api/identifier.service'
import DatabaseService from '@/api/database.service'
import UserMapper from '@/api/user.mapper'

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
        titles: [],
        descriptions: [],
        visibility: 'everyone',
        publisher: this.$config.defaultPublisher,
        publication_year: formatYearUTC(Date.now()),
        publication_month: formatMonthUTC(Date.now()),
        publication_day: formatDayUTC(Date.now()),
        license: null,
        type: this.type,
        creators: [],
        related_identifiers: []
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
      languages: [
        { value: 'ab' },
        { value: 'aa' },
        { value: 'af' },
        { value: 'ak' },
        { value: 'sq' },
        { value: 'am' },
        { value: 'ar' },
        { value: 'an' },
        { value: 'hy' },
        { value: 'as' },
        { value: 'av' },
        { value: 'ae' },
        { value: 'ay' },
        { value: 'az' },
        { value: 'bm' },
        { value: 'ba' },
        { value: 'eu' },
        { value: 'be' },
        { value: 'bn' },
        { value: 'bh' },
        { value: 'bi' },
        { value: 'bs' },
        { value: 'br' },
        { value: 'bg' },
        { value: 'my' },
        { value: 'ca' },
        { value: 'km' },
        { value: 'ch' },
        { value: 'ce' },
        { value: 'ny' },
        { value: 'zh' },
        { value: 'cu' },
        { value: 'cv' },
        { value: 'kw' },
        { value: 'co' },
        { value: 'cr' },
        { value: 'hr' },
        { value: 'cs' },
        { value: 'da' },
        { value: 'dv' },
        { value: 'nl' },
        { value: 'dz' },
        { value: 'en' },
        { value: 'eo' },
        { value: 'et' },
        { value: 'ee' },
        { value: 'fo' },
        { value: 'fj' },
        { value: 'fi' },
        { value: 'fr' },
        { value: 'ff' },
        { value: 'gd' },
        { value: 'gl' },
        { value: 'lg' },
        { value: 'ka' },
        { value: 'de' },
        { value: 'ki' },
        { value: 'el' },
        { value: 'kl' },
        { value: 'gn' },
        { value: 'gu' },
        { value: 'ht' },
        { value: 'ha' },
        { value: 'he' },
        { value: 'hz' },
        { value: 'hi' },
        { value: 'ho' },
        { value: 'hu' },
        { value: 'is' },
        { value: 'io' },
        { value: 'ig' },
        { value: 'id' },
        { value: 'ia' },
        { value: 'ie' },
        { value: 'iu' },
        { value: 'ik' },
        { value: 'ga' },
        { value: 'it' },
        { value: 'ja' },
        { value: 'jv' },
        { value: 'kn' },
        { value: 'kr' },
        { value: 'ks' },
        { value: 'kk' },
        { value: 'rw' },
        { value: 'kv' },
        { value: 'kg' },
        { value: 'ko' },
        { value: 'kj' },
        { value: 'ku' },
        { value: 'ky' },
        { value: 'lo' },
        { value: 'la' },
        { value: 'lv' },
        { value: 'lb' },
        { value: 'li' },
        { value: 'ln' },
        { value: 'lt' },
        { value: 'lu' },
        { value: 'mk' },
        { value: 'mg' },
        { value: 'ms' },
        { value: 'ml' },
        { value: 'mt' },
        { value: 'gv' },
        { value: 'mi' },
        { value: 'mr' },
        { value: 'mh' },
        { value: 'ro' },
        { value: 'mn' },
        { value: 'na' },
        { value: 'nv' },
        { value: 'nd' },
        { value: 'ng' },
        { value: 'ne' },
        { value: 'se' },
        { value: 'no' },
        { value: 'nb' },
        { value: 'nn' },
        { value: 'ii' },
        { value: 'oc' },
        { value: 'oj' },
        { value: 'or' },
        { value: 'om' },
        { value: 'os' },
        { value: 'pi' },
        { value: 'pa' },
        { value: 'ps' },
        { value: 'fa' },
        { value: 'pl' },
        { value: 'pt' },
        { value: 'qu' },
        { value: 'rm' },
        { value: 'rn' },
        { value: 'ru' },
        { value: 'sm' },
        { value: 'sg' },
        { value: 'sa' },
        { value: 'sc' },
        { value: 'sr' },
        { value: 'sn' },
        { value: 'sd' },
        { value: 'si' },
        { value: 'sk' },
        { value: 'sl' },
        { value: 'so' },
        { value: 'st' },
        { value: 'nr' },
        { value: 'es' },
        { value: 'su' },
        { value: 'sw' },
        { value: 'ss' },
        { value: 'sv' },
        { value: 'tl' },
        { value: 'ty' },
        { value: 'tg' },
        { value: 'ta' },
        { value: 'tt' },
        { value: 'te' },
        { value: 'th' },
        { value: 'bo' },
        { value: 'ti' },
        { value: 'to' },
        { value: 'ts' },
        { value: 'tn' },
        { value: 'tr' },
        { value: 'tk' },
        { value: 'tw' },
        { value: 'ug' },
        { value: 'uk' },
        { value: 'ur' },
        { value: 'uz' },
        { value: 've' },
        { value: 'vi' },
        { value: 'vo' },
        { value: 'wa' },
        { value: 'cy' },
        { value: 'fy' },
        { value: 'wo' },
        { value: 'xh' },
        { value: 'yi' },
        { value: 'yo' },
        { value: 'za' },
        { value: 'zu' }
      ],
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
    backTo () {
      return `/database/${this.$route.params.database_id}` + (this.isSubset ? `/query/${this.$route.params.query_id}` : '')
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
    this.addCreator()
    this.addTitle()
    this.addDescription()
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
      if (!creator || !creator.name_identifier) {
        return
      }
      creator.loading = true
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
            creator.affiliation = metadata.affiliations.length > 0 ? metadata.affiliations[0].organization_name : null
          }
          creator.name_identifier_scheme = UserMapper.nameIdentifierToNameIdentifierScheme(creator.name_identifier)
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
        affiliation_identifier: null,
        name_identifier: null,
        name_identifier_scheme: null,
        name_type: 'Personal',
        creator_name: null,
        loading: false /* removed later */,
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
    persist () {
      this.loading = true
      const payload = Object.assign({}, this.identifier)
      payload.creators.forEach((c) => {
        delete c.loading
        delete c.success
      })
      IdentifierService.create(payload)
        .then(() => {
          this.$toast.success(this.prefix + ' successfully persisted')
          this.$store.dispatch('reloadDatabase')
          this.$router.push(this.backTo)
        })
        .finally(() => {
          this.loading = false
        })
    },
    update () {
      // this.loading = true
      // const payload = {
      //   dbid: parseInt(this.$route.params.database_id),
      //   qid: parseInt(this.$route.params.query_id),
      //   title: this.identifier.title,
      //   description: this.identifier.description,
      //   publisher: this.identifier.publisher,
      //   publication_year: this.identifier.publication_year,
      //   publication_month: this.identifier.publication_month,
      //   publication_day: this.identifier.publication_day,
      //   license: this.identifier.license,
      //   type: this.identifier.type,
      //   creators: this.identifier.creators,
      //   related_identifiers: this.identifier.related_identifiers
      // }
      // IdentifierService.update(this.identifier.id, payload)
      //   .then(() => {
      //     this.$toast.success(this.prefix + ' identifier successfully updated')
      //     this.$emit('close', { action: 'persisted' })
      //   })
      //   .finally(() => {
      //     this.loading = false
      //   })
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
.v-stepper .v-stepper__step--active .v-stepper__label {
  text-shadow: none !important;
}
</style>
