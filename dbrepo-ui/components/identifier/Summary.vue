<template>
  <v-card flat tile>
    <v-card-title>Identifier</v-card-title>
    <v-card-text>
      <v-list dense>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mt-2">
              Persistent Identifier
            </v-list-item-title>
            <v-list-item-content>
              <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
              <Banner v-if="!loading" :identifier="database.identifier" />
            </v-list-item-content>
            <v-list-item-title class="mt-2">
              Database Title
            </v-list-item-title>
            <v-list-item-content v-for="(title,i) in identifier.titles" :key="`t-${i}`">
              <span>{{ printTitle(title) }} <sup v-if="printTitleLang(title)" v-text="printTitleLang(title)" /></span>
            </v-list-item-content>
            <v-list-item-title class="mt-2">
              Database Description
            </v-list-item-title>
            <v-list-item-content>
              <v-list-item-content v-for="(description,i) in identifier.descriptions" :key="`d-${i}`">
                <span>{{ printDescription(description) }} <sup v-if="printDescriptionLang(description)" v-text="printDescriptionLang(description)" /></span>
              </v-list-item-content>
            </v-list-item-content>
            <v-list-item-title class="mt-2">
              Database Publisher
            </v-list-item-title>
            <v-list-item-content>
              {{ database.identifier.publisher }}
            </v-list-item-content>
            <v-list-item-title v-if="identifier.creators.length > 0" class="mt-2">
              Creators
            </v-list-item-title>
            <v-list-item-content>
              <p v-for="(person_or_org, i) in identifier.creators" :key="`c-${i}`" class="mt-2">
                <OrcidIcon v-if="person_or_org.name_identifier && person_or_org.name_identifier_scheme === 'ORCID'" :orcid="person_or_org.name_identifier" />
                <IsniIcon v-if="person_or_org.name_identifier && person_or_org.name_identifier_scheme === 'ISNI'" :isni="person_or_org.name_identifier" />
                <RorIcon v-if="person_or_org.name_identifier && person_or_org.name_identifier_scheme === 'ROR'" :ror="person_or_org.name_identifier" />
                <span v-text="person_or_org.creator_name" />
                <sup v-if="person_or_org.affiliation" v-text="person_or_org.affiliation" />
              </p>
              <span v-for="(affiliation, i) in identifier.affiliations" :key="`a-${i}`" class="mt-4">
                <span>
                  <sup>{{ i+1 }}</sup>
                  {{ affiliation }}
                </span>
              </span>
            </v-list-item-content>
            <v-list-item-title v-if="identifier.language" class="mt-2">
              Language
            </v-list-item-title>
            <v-list-item-content v-if="identifier.language">
              <span v-if="!loading" v-text="identifier.language" />
            </v-list-item-content>
            <v-list-item-title v-if="publication" class="mt-2">
              Publication Date
            </v-list-item-title>
            <v-list-item-content v-if="publication">
              <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
              <span v-if="!loading" v-text="publication" />
            </v-list-item-content>
            <v-list-item-title v-if="identifier.related.length > 0" class="mt-2">
              Related Identifiers
            </v-list-item-title>
            <v-list-item-content v-if="identifier.related.length > 0">
              <div v-for="(rel, i) in identifier.related" :key="`r-${i}`">
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
            <v-list-item-title v-if="identifier.license" class="mt-2">
              License
            </v-list-item-title>
            <v-list-item-content v-if="identifier.license">
              <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
              <a v-if="identifier.license" target="_blank" :href="identifier.license.uri">{{ identifier.license.identifier }}</a>
              <span v-if="!identifier.license">(none)</span>
            </v-list-item-content>
            <Citation :pid="database.identifier.id" />
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
import { formatDateUTC } from '@/utils'

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
      return location.protocol + '//' + location.host
    },
    access () {
      return this.$store.state.access
    },
    database () {
      return this.$store.state.database
    },
    pid () {
      return `${this.baseUrl}/pid/${this.database.identifier.id}`
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
    printTitle (title) {
      return (title.type ? title.type + ': ' : '') + title.title
    },
    printTitleLang (title) {
      return title.language ? title.language.toUpperCase() : null
    },
    printDescription (description) {
      return (description.type ? description.type + ': ' : '') + description.description
    },
    printDescriptionLang (description) {
      return description.language ? description.language.toUpperCase() : null
    }
  }
}
</script>
