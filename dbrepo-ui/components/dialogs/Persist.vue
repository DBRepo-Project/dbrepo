<template>
  <div>
    <v-card>
      <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
      <v-card-title v-text="`Persist ${title}`" />
      <v-card-text>
        <v-alert
          v-if="isSubset"
          border="left"
          color="info">
          Choose an expressive subset title and describe what it produces.
        </v-alert>
        <v-form v-model="formValid" autocomplete="off">
          <v-row dense>
            <v-col>
              <v-text-field
                id="title"
                v-model="identifier.title"
                name="title"
                :label="`${prefix} title *`"
                :rules="[v => !!v || $t('Required')]"
                required />
              <v-textarea
                id="description"
                v-model="identifier.description"
                name="description"
                rows="2"
                :label="`${prefix} description *`" />
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
          <v-row v-for="(creator,i) in identifier.creators" :key="`c-${i}`" dense>
            <v-col cols="3">
              <v-text-field
                v-model="creator.firstname"
                name="firstname"
                label="Firstname *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="creator.lastname"
                name="lastname"
                label="Lastname *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="creator.affiliation"
                name="affiliation"
                label="Affiliation *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col cols="2">
              <v-text-field
                v-model="creator.orcid"
                name="orcid"
                label="ORCID" />
            </v-col>
            <v-col cols="1" class="mt-5">
              <v-btn v-if="!i" icon x-small @click="deleteCreator(i)">
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
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          class="mb-2"
          @click="cancel">
          Cancel
        </v-btn>
        <v-btn
          class="mb-2"
          :disabled="!formValid || loading"
          color="primary"
          @click="persist">
          Persist
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import { formatYearUTC, formatMonthUTC, formatDayUTC } from '@/utils'
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
      visibility: 'Public',
      identifier: {
        cid: parseInt(this.$route.params.container_id),
        dbid: parseInt(this.$route.params.database_id),
        qid: parseInt(this.$route.params.query_id),
        title: null,
        description: null,
        publisher: null,
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
            orcid: null
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
    config () {
      if (this.token === null) {
        return {
          headers: { Accept: 'application/json' }
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}`, Accept: 'application/json' }
      }
    },
    isSubset () {
      return this.type === 'subset'
    },
    isDatabase () {
      return this.type === 'database'
    },
    title () {
      if (this.isSubset) {
        return 'subset'
      } else if (this.isDatabase) {
        return 'database'
      }
      return ''
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
  mounted () {
    this.loadLicenses()
  },
  methods: {
    cancel () {
      this.$parent.$parent.$parent.persistQueryDialog = false
      this.$emit('close', { action: 'closed' })
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
      this.identifier.creators.splice(index, 1)
    },
    deleteRelatedIdentifier (index) {
      this.identifier.related_identifiers.splice(index, 1)
    },
    async persist () {
      this.loading = true
      let res
      try {
        res = await this.$axios.post('/api/identifier', this.identifier, this.config)
        console.debug('persist', res.data)
        await this.loadDatabase()
      } catch (err) {
        this.error = true
        this.loading = false
        this.$toast.error('Failed to persist')
        console.error('persist failed', err)
        return
      }
      this.$toast.success(this.prefix + ' successfully persisted')
      this.$emit('close', { action: 'persisted' })
      this.loading = false
    },
    async loadLicenses () {
      if (!this.token) {
        return
      }
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/license`, this.config)
        this.licenses = res.data
        console.debug('license', this.licenses)
      } catch (error) {
        console.error('Failed load licenses', error)
        const { data } = error.response
        const { message } = data
        this.$toast.error(`Failed load licenses: ${message}`)
      }
      this.loading = false
    },
    async loadDatabase () {
      if (!this.$route.params.container_id || !this.$route.params.database_id) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.$store.commit('SET_DATABASE', res.data)
        console.debug('database', this.database)
      } catch (err) {
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
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
