<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card flat>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title>
          Persist Database
        </v-card-title>
        <v-card-text>
          <v-alert
            border="left"
            color="info">
            Choose an expressive database description for the information stored.
          </v-alert>
          <v-row dense>
            <v-col>
              <v-text-field
                id="title"
                v-model="identifier.title"
                name="title"
                label="Title *"
                disabled
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="publisher"
                v-model="identifier.publisher"
                name="publisher"
                label="Publisher *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-textarea
                id="description"
                v-model="identifier.description"
                name="description"
                rows="2"
                label="Description" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                id="language"
                v-model="identifier.language"
                name="language"
                label="Language"
                :items="languages"
                clearable
                item-value="value"
                item-text="text" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="4">
              <v-text-field
                id="publication-day"
                v-model.number="identifier.publication_day"
                name="publication-day"
                label="Publication Day"
                hint="e.g. 08"
                type="number"
                clearable
                min="1"
                max="31" />
            </v-col>
            <v-col cols="4">
              <v-text-field
                id="publication-month"
                v-model.number="identifier.publication_month"
                name="publication-month"
                label="Publication Month"
                hint="e.g. 12"
                type="number"
                clearable
                min="1"
                max="12" />
            </v-col>
            <v-col cols="4">
              <v-text-field
                id="publication-year"
                v-model.number="identifier.publication_year"
                name="publication-year"
                label="Publication Year *"
                hint="e.g. 2022"
                type="number"
                clearable
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                id="license"
                v-model="identifier.license"
                name="license"
                label="License"
                :items="licenses"
                clearable
                item-value="identifier"
                item-text="identifier"
                return-object />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-select
                v-if="false"
                id="contact"
                v-model="identifier.contact_person"
                name="contact"
                label="Contact"
                :items="users"
                clearable
                item-value="username"
                :item-text="item => `${printUser(item)}`" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Close
          </v-btn>
          <v-btn
            id="database"
            class="mb-2 mr-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            @click="persist">
            Persist
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { formatDayUTC, formatMonthUTC, formatUser, formatYearUTC } from '@/utils'
export default {
  props: {
    database: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      menu: false,
      users: [],
      identifier: {
        cid: parseInt(this.$route.params.container_id),
        dbid: parseInt(this.$route.params.database_id),
        title: null,
        publisher: 'TU Wien',
        description: null,
        publication_year: formatYearUTC(Date.now()),
        publication_month: formatMonthUTC(Date.now()),
        publication_day: formatDayUTC(Date.now()),
        type: 'database',
        visibility: 'everyone',
        doi: null,
        creators: [],
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
      ],
      licenses: [],
      languages: [
        { text: 'aa', value: 'aa' },
        { text: 'ab', value: 'ab' },
        { text: 'ae', value: 'ae' },
        { text: 'af', value: 'af' },
        { text: 'ak', value: 'ak' },
        { text: 'am', value: 'am' },
        { text: 'an', value: 'an' },
        { text: 'ar', value: 'ar' },
        { text: 'as', value: 'as' },
        { text: 'av', value: 'av' },
        { text: 'ay', value: 'ay' },
        { text: 'az', value: 'az' },
        { text: 'ba', value: 'ba' },
        { text: 'be', value: 'be' },
        { text: 'bg', value: 'bg' },
        { text: 'bh', value: 'bh' },
        { text: 'bi', value: 'bi' },
        { text: 'bm', value: 'bm' },
        { text: 'bn', value: 'bn' },
        { text: 'bo', value: 'bo' },
        { text: 'br', value: 'br' },
        { text: 'bs', value: 'bs' },
        { text: 'ca', value: 'ca' },
        { text: 'ce', value: 'ce' },
        { text: 'ch', value: 'ch' },
        { text: 'co', value: 'co' },
        { text: 'cr', value: 'cr' },
        { text: 'cs', value: 'cs' },
        { text: 'cu', value: 'cu' },
        { text: 'cv', value: 'cv' },
        { text: 'cy', value: 'cy' },
        { text: 'da', value: 'da' },
        { text: 'de', value: 'de' },
        { text: 'dv', value: 'dv' },
        { text: 'dz', value: 'dz' },
        { text: 'ee', value: 'ee' },
        { text: 'el', value: 'el' },
        { text: 'en', value: 'en' },
        { text: 'eo', value: 'eo' },
        { text: 'es', value: 'es' },
        { text: 'et', value: 'et' },
        { text: 'eu', value: 'eu' },
        { text: 'fa', value: 'fa' },
        { text: 'ff', value: 'ff' },
        { text: 'fi', value: 'fi' },
        { text: 'fj', value: 'fj' },
        { text: 'fo', value: 'fo' },
        { text: 'fr', value: 'fr' },
        { text: 'fy', value: 'fy' },
        { text: 'ga', value: 'ga' },
        { text: 'gd', value: 'gd' },
        { text: 'gl', value: 'gl' },
        { text: 'gn', value: 'gn' },
        { text: 'gu', value: 'gu' },
        { text: 'gv', value: 'gv' },
        { text: 'ha', value: 'ha' },
        { text: 'he', value: 'he' },
        { text: 'hi', value: 'hi' },
        { text: 'ho', value: 'ho' },
        { text: 'hr', value: 'hr' },
        { text: 'ht', value: 'ht' },
        { text: 'hu', value: 'hu' },
        { text: 'hy', value: 'hy' },
        { text: 'hz', value: 'hz' },
        { text: 'ia', value: 'ia' },
        { text: 'id', value: 'id' },
        { text: 'ie', value: 'ie' },
        { text: 'ig', value: 'ig' },
        { text: 'ii', value: 'ii' },
        { text: 'ik', value: 'ik' },
        { text: 'io', value: 'io' },
        { text: 'is', value: 'is' },
        { text: 'it', value: 'it' },
        { text: 'iu', value: 'iu' },
        { text: 'ja', value: 'ja' },
        { text: 'jv', value: 'jv' },
        { text: 'ka', value: 'ka' },
        { text: 'kg', value: 'kg' },
        { text: 'ki', value: 'ki' },
        { text: 'kj', value: 'kj' },
        { text: 'kk', value: 'kk' },
        { text: 'kl', value: 'kl' },
        { text: 'km', value: 'km' },
        { text: 'kn', value: 'kn' },
        { text: 'ko', value: 'ko' },
        { text: 'kr', value: 'kr' },
        { text: 'ks', value: 'ks' },
        { text: 'ku', value: 'ku' },
        { text: 'kv', value: 'kv' },
        { text: 'kw', value: 'kw' },
        { text: 'ky', value: 'ky' },
        { text: 'la', value: 'la' },
        { text: 'lb', value: 'lb' },
        { text: 'lg', value: 'lg' },
        { text: 'li', value: 'li' },
        { text: 'ln', value: 'ln' },
        { text: 'lo', value: 'lo' },
        { text: 'lt', value: 'lt' },
        { text: 'lu', value: 'lu' },
        { text: 'lv', value: 'lv' },
        { text: 'mg', value: 'mg' },
        { text: 'mh', value: 'mh' },
        { text: 'mi', value: 'mi' },
        { text: 'mk', value: 'mk' },
        { text: 'ml', value: 'ml' },
        { text: 'mn', value: 'mn' },
        { text: 'mr', value: 'mr' },
        { text: 'ms', value: 'ms' },
        { text: 'mt', value: 'mt' },
        { text: 'my', value: 'my' },
        { text: 'na', value: 'na' },
        { text: 'nb', value: 'nb' },
        { text: 'nd', value: 'nd' },
        { text: 'ne', value: 'ne' },
        { text: 'ng', value: 'ng' },
        { text: 'nl', value: 'nl' },
        { text: 'nn', value: 'nn' },
        { text: 'no', value: 'no' },
        { text: 'nr', value: 'nr' },
        { text: 'nv', value: 'nv' },
        { text: 'ny', value: 'ny' },
        { text: 'oc', value: 'oc' },
        { text: 'oj', value: 'oj' },
        { text: 'om', value: 'om' },
        { text: 'or', value: 'or' },
        { text: 'os', value: 'os' },
        { text: 'pa', value: 'pa' },
        { text: 'pi', value: 'pi' },
        { text: 'pl', value: 'pl' },
        { text: 'ps', value: 'ps' },
        { text: 'pt', value: 'pt' },
        { text: 'qu', value: 'qu' },
        { text: 'rm', value: 'rm' },
        { text: 'rn', value: 'rn' },
        { text: 'ro', value: 'ro' },
        { text: 'ru', value: 'ru' },
        { text: 'rw', value: 'rw' },
        { text: 'sa', value: 'sa' },
        { text: 'sc', value: 'sc' },
        { text: 'sd', value: 'sd' },
        { text: 'se', value: 'se' },
        { text: 'sg', value: 'sg' },
        { text: 'si', value: 'si' },
        { text: 'sk', value: 'sk' },
        { text: 'sl', value: 'sl' },
        { text: 'sm', value: 'sm' },
        { text: 'sn', value: 'sn' },
        { text: 'so', value: 'so' },
        { text: 'sq', value: 'sq' },
        { text: 'sr', value: 'sr' },
        { text: 'ss', value: 'ss' },
        { text: 'st', value: 'st' },
        { text: 'su', value: 'su' },
        { text: 'sv', value: 'sv' },
        { text: 'sw', value: 'sw' },
        { text: 'ta', value: 'ta' },
        { text: 'te', value: 'te' },
        { text: 'tg', value: 'tg' },
        { text: 'th', value: 'th' },
        { text: 'ti', value: 'ti' },
        { text: 'tk', value: 'tk' },
        { text: 'tl', value: 'tl' },
        { text: 'tn', value: 'tn' },
        { text: 'to', value: 'to' },
        { text: 'tr', value: 'tr' },
        { text: 'ts', value: 'ts' },
        { text: 'tt', value: 'tt' },
        { text: 'tw', value: 'tw' },
        { text: 'ty', value: 'ty' },
        { text: 'ug', value: 'ug' },
        { text: 'uk', value: 'uk' },
        { text: 'ur', value: 'ur' },
        { text: 'uz', value: 'uz' },
        { text: 've', value: 've' },
        { text: 'vi', value: 'vi' },
        { text: 'vo', value: 'vo' },
        { text: 'wa', value: 'wa' },
        { text: 'wo', value: 'wo' },
        { text: 'xh', value: 'xh' },
        { text: 'yi', value: 'yi' },
        { text: 'yo', value: 'yo' },
        { text: 'za', value: 'za' },
        { text: 'zh', value: 'zh' },
        { text: 'zu', value: 'zu' }
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
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    }
  },
  mounted () {
    this.loadLicenses()
    this.loadUsers()
    this.identifier.title = this.database.name
    this.identifier.publication_year = parseInt(new Date().getFullYear())
  },
  methods: {
    printUser (item) {
      return formatUser(item)
    },
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog', { success: false })
    },
    reset () {
      this.menu = false
    },
    async loadLicenses () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/license`)
        this.licenses = res.data
        console.debug('licenses', this.licenses)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to fetch licenses')
      }
      this.loading = false
    },
    async loadUsers () {
      try {
        this.loading = true
        const res = await this.$axios.get('/api/user')
        this.users = res.data
        console.debug('users', this.users)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to fetch users')
      }
      this.loading = false
    },
    async persist () {
      this.loading = true
      try {
        this.loading = true
        const res = await this.$axios.post('/api/identifier', this.identifier, this.config)
        console.debug('persist', res.data)
        this.$toast.success('Database persisted.')
        this.$emit('close-dialog', { action: 'persisted', success: true })
      } catch (err) {
        this.error = true
        this.loading = false
        this.$toast.error('Failed to persist database')
        console.error('persist failed', err)
        return
      }
      this.loading = false
    }
  }
}
</script>
<style scoped>
</style>
