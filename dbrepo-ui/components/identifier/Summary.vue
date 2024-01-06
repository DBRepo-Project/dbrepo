<template>
  <v-card flat tile>
    <v-card-title>Identifier</v-card-title>
    <v-card-text v-if="identifier">
      <v-list dense>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mt-2">
              Persistent Identifier
            </v-list-item-title>
            <v-list-item-content>
              <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
              <Banner v-if="!loading" :identifier="identifier" />
            </v-list-item-content>
            <div v-for="(title,i) in identifier.titles" :key="`t-${i}`">
              <v-list-item-title class="mt-2">
                {{ printType }} {{ title.type ? title.type : 'Title' }} <span v-if="printTitleLang(title)" v-text="`(${printTitleLang(title)})`" />
              </v-list-item-title>
              <v-list-item-content v-text="title.title" />
            </div>
            <div v-for="(description,i) in identifier.descriptions" :key="`d-${i}`">
              <v-list-item-title class="mt-2">
                {{ printType }} {{ description.type ? description.type : 'Description' }} <span v-if="printDescriptionLang(description)" v-text="`(${printDescriptionLang(description)})`" />
              </v-list-item-title>
              <v-list-item-content v-text="description.description" />
            </div>
            <v-list-item-title class="mt-2" v-text="`${printType} Publisher`" />
            <v-list-item-content>
              {{ identifier.publisher }}
            </v-list-item-content>
            <v-list-item-title v-if="identifier.creators && identifier.creators.length > 0" class="mt-2" v-text="`${printType} Creators`" />
            <v-list-item-content>
              <p v-for="(personOrOrg, i) in identifier.creators" :key="`c-${i}`" class="mt-2">
                <OrcidIcon v-if="hasOrcid(personOrOrg)" :orcid="personOrOrg.name_identifier" />
                <IsniIcon v-if="hasIsni(personOrOrg)" :isni="personOrOrg.name_identifier" />
                <RorIcon v-if="hasRor(personOrOrg)" :ror="personOrOrg.name_identifier" />
                <span v-text="personOrOrg.creator_name" />
                <sup v-if="hasAffiliation(personOrOrg)">
                  <a v-if="personOrOrg.affiliation_identifier" :href="personOrOrg.affiliation_identifier" target="_blank">
                    {{ personOrOrg.affiliation ? personOrOrg.affiliation : personOrOrg.affiliation_identifier }}
                  </a>
                </sup>
              </p>
            </v-list-item-content>
            <v-list-item-title v-if="identifierLang" class="mt-2">
              Language
            </v-list-item-title>
            <v-list-item-content v-if="identifierLang" v-text="identifierLang" />
            <v-list-item-title v-if="publication" class="mt-2">
              Publication Date
            </v-list-item-title>
            <v-list-item-content v-text="publication" />
            <v-list-item-title v-if="identifier.related_identifiers && identifier.related_identifiers.length > 0" class="mt-2">
              Related Identifiers
            </v-list-item-title>
            <v-list-item-content v-if="identifier.related_identifiers && identifier.related_identifiers.length > 0">
              <div v-for="(rel, i) in identifier.related_identifiers" :key="`r-${i}`">
                <span v-if="rel.type === 'DOI'">
                  {{ rel.type }}: <a :href="`https://doi.org/${rel.value}`" target="_blank">{{ rel.value }}</a>
                  <span v-if="rel.relation">({{ rel.relation }})</span>
                </span>
                <span v-if="rel.type === 'URL'">
                  {{ rel.type }}: <a :href="`${rel.value}`" target="_blank">{{ rel.value }}</a>
                  <span v-if="rel.relation">({{ rel.relation }})</span>
                </span>
                <span v-if="rel.type === 'arXiv'">
                  {{ rel.type }}: <a :href="`https://arxiv.org/abs/${rel.value}`" target="_blank">{{ rel.value }}</a>
                  <span v-if="rel.relation">({{ rel.relation }})</span>
                </span>
                <span v-if="rel.type === 'EISSN'">
                  {{ rel.type }}: <a :href="`https://portal.issn.org/resource/ISSN/${rel.value}`" target="_blank">{{ rel.value }}</a>
                  <span v-if="rel.relation">({{ rel.relation }})</span>
                </span>
                <span v-if="rel.type !== 'DOI' && rel.type !== 'URL' && rel.type !== 'arXiv' && rel.type !== 'EISSN'">
                  {{ rel.type }}: {{ rel.value }}
                  <span v-if="rel.relation">({{ rel.relation }})</span>
                </span>
              </div>
            </v-list-item-content>
            <v-list-item-title v-if="identifier.funders && identifier.funders.length > 0" class="mt-2">
              Funding Information
            </v-list-item-title>
            <v-list-item-content v-if="funding" v-text="funding" />
            <v-list-item-title v-if="hasLicenses" class="mt-2" v-text="licensesHeading" />
            <v-list-item-content v-if="hasLicenses" style="display:inline;">
              <span v-for="(license,i) in identifier.licenses" :key="i">
                {{ i > 0 ? ', ' : '' }}
                <a v-if="license" target="_blank" :href="license.uri">{{ license.identifier }}</a>
              </span>
            </v-list-item-content>
            <Citation :identifier="identifier" />
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-card-text>
  </v-card>
</template>
<script>
import Citation from '@/components/identifier/Citation.vue'
import IsniIcon from '@/components/icons/IsniIcon.vue'
import OrcidIcon from '@/components/icons/OrcidIcon.vue'
import RorIcon from '@/components/icons/RorIcon.vue'
import Banner from '@/components/identifier/Banner.vue'
import { formatDateUTC, formatLanguage } from '@/utils'

export default {
  components: {
    Citation,
    IsniIcon,
    OrcidIcon,
    RorIcon,
    Banner
  },
  props: {
    identifier: {
      type: Object,
      default () {
        return null
      }
    }
  },
  data () {
    return {
      loading: false
    }
  },
  computed: {
    baseUrl () {
      return `${location.protocol}//${location.host}`
    },
    access () {
      return this.$store.state.access
    },
    database () {
      return this.$store.state.database
    },
    printType () {
      return this.identifier.type === 'database' ? 'Database' : 'Subset'
    },
    pid () {
      return `${this.baseUrl}/pid/${this.database.identifier.id}`
    },
    identifierLang () {
      return this.identifier.language ? formatLanguage(this.identifier.language.toLowerCase()) : null
    },
    licensesHeading () {
      if (!this.identifier.licenses) {
        return null
      }
      return 'License' + (this.identifier.licenses.length > 1 ? 's' : '')
    },
    hasLicenses () {
      return this.identifier.licenses && this.identifier.licenses.length > 0
    },
    funding () {
      if (!this.identifier.funders || this.identifier.funders.length === 0) {
        return null
      }
      let text = ''
      for (let i = 0; i < this.identifier.funders.length; i++) {
        const funder = this.identifier.funders[i]
        text += ((i > 0) ? ', it has also received' : 'The project associated with this data has received')
        text += (' funding from the ' + funder.funder_name)
        text += ((funder.award_number ? ' under grant agreement number ' + funder.award_number : ''))
        text += ((funder.award_title ? ' (' + funder.award_title + ')' : ''))
      }
      text += '.'
      return text
    },
    publication () {
      if (this.identifier.publication_year && !this.identifier.publication_month && !this.identifier.publication_day) {
        return this.identifier.publication_year
      } else if (this.identifier.publication_year && this.identifier.publication_month && this.identifier.publication_day) {
        return formatDateUTC(this.identifier.publication_year + '-' + this.identifier.publication_month + '-' + this.identifier.publication_day)
      } else {
        return null
      }
    }
  },
  methods: {
    printTitleLang (title) {
      return title.language ? formatLanguage(title.language.toLowerCase()) : null
    },
    printDescriptionLang (description) {
      return description.language ? formatLanguage(description.language.toLowerCase()) : null
    },
    hasOrcid (personOrOrg) {
      return personOrOrg.name_identifier && personOrOrg.name_identifier_scheme === 'ORCID'
    },
    hasIsni (personOrOrg) {
      return personOrOrg.name_identifier && personOrOrg.name_identifier_scheme === 'ISNI'
    },
    hasRor (personOrOrg) {
      return personOrOrg.name_identifier && personOrOrg.name_identifier_scheme === 'ROR'
    },
    hasAffiliation (personOrOrg) {
      return personOrOrg.affiliation || personOrOrg.affiliation_identifier
    },
    formatLanguage
  }
}
</script>
