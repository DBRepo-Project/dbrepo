<template>
  <v-card
    :title="$t('pages.identifier.title')"
    variant="flat"
    rounded="0">
    <v-card-text>
      <v-list
        lines="two"
        dense>
        <v-list-item
          :title="$t('pages.identifier.pid.title')"
          density="compact">
          <Banner v-if="!loading" :identifier="identifier" />
        </v-list-item>
        <v-list-item
          :title="$t('pages.identifier.titles.title')"
          density="compact">
          <p
            v-for="(title, i) in identifier.titles"
            :key="`t-${i}`">
            <span>
              <v-badge
                v-if="title.language"
                inline
                :content="title.language"
                color="code">
                <span v-text="title.title" />
              </v-badge>
              <span v-else v-text="title.title" />
            </span>
          </p>
        </v-list-item>
        <v-list-item
          :title="$t('pages.identifier.descriptions.title')"
          density="compact">
          <p
            v-for="(description, i) in identifier.descriptions"
            :key="`d-${i}`">
            <span>
              <v-badge
                v-if="description.language"
                inline
                :content="description.language"
                color="code">
                <span v-text="description.description" />
              </v-badge>
              <span v-else v-text="description.description" />
            </span>
          </p>
        </v-list-item>
        <v-list-item
          :title="$t('pages.identifier.publisher.title')"
          density="compact">
          <div v-text="identifier.publisher" />
        </v-list-item>
        <v-list-item
          :title="$t('pages.identifier.creators.title')"
          density="compact">
          <p
            v-for="(personOrOrg, i) in identifier.creators"
            :key="`c-${i}`">
            <OrcidIcon
              v-if="hasOrcid(personOrOrg)"
              class="mr-1"
              :orcid="personOrOrg.name_identifier" />
            <IsniIcon
              v-if="hasIsni(personOrOrg)"
              class="mr-1"
              :isni="personOrOrg.name_identifier" />
            <RorIcon
              v-if="hasRor(personOrOrg)"
              class="mr-1"
              :ror="personOrOrg.name_identifier" />
            <span
              v-text="personOrOrg.creator_name" />
            <sup
              v-if="hasAffiliation(personOrOrg)"
              class="ml-1">
              <a
                v-if="personOrOrg.affiliation_identifier"
                :href="personOrOrg.affiliation_identifier">
                {{ personOrOrg.affiliation ? personOrOrg.affiliation : personOrOrg.affiliation_identifier }}
              </a>
            </sup>
          </p>
        </v-list-item>
        <v-list-item
          v-if="identifierLang"
          :title="$t('pages.identifier.language.title')"
          density="compact">
          <div v-text="identifierLang" />
        </v-list-item>
        <v-list-item
          v-if="publication"
          :title="$t('pages.identifier.publication-date.title')"
          density="compact">
          <div v-text="publication" />
        </v-list-item>
        <v-list-item
          v-if="identifier.related_identifiers && identifier.related_identifiers.length > 0"
          :title="$t('pages.identifier.related-identifiers.title')"
          density="compact">
          <p
            v-for="(related, i) in identifier.related_identifiers"
            :key="`r-${i}`">
            <span v-text="`${related.type}:`" />
            <a
              v-if="related.value.startsWith('http')"
              :href="related.value"
              v-text="related.value"
              class="ml-1" />
            <span
              v-else
              class="ml-1"
              v-text="related.value" />
            <span
              v-if="related.relation"
              class="ml-1"
              v-text="`(${related.relation})`"/>
          </p>
        </v-list-item>
        <v-list-item
          v-if="identifier.funders && identifier.funders.length > 0"
          :title="$t('pages.identifier.funders.title')"
          density="compact">
          <p
            v-for="(funder, i) in identifier.funders"
            :key="`f-${i}`">
            <a
              v-if="funder.funder_identifier"
              v-text="funder.funder_name"
              :href="funder.funder_identifier" />
            <span
              v-if="funder.award_title"
              class="ml-1"
              v-text="funder.award_title" />
            <span
              v-if="funder.award_number"
              class="ml-1"
              v-text="`(${funder.award_number})`" />
          </p>
        </v-list-item>
        <v-list-item
          v-if="hasLicenses"
          :title="$t('pages.identifier.licenses.title')"
          density="compact">
          <p
            v-for="(license, i) in identifier.licenses"
            :key="`l-${i}`">
            <span>
              <span v-text="i > 0 ? ', ' : ''" />
              <a
                v-if="license"
                v-text="license.identifier"
                :href="license.uri" />
            </span>
          </p>
        </v-list-item>
        <v-list-item
          :title="$t('pages.identifier.citation.title')"
          density="compact">
          <Citation
            :identifier="identifier" />
        </v-list-item>
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script>
import Citation from '@/components/identifier/Citation'
import IsniIcon from '@/components/icons/IsniIcon'
import OrcidIcon from '@/components/icons/OrcidIcon'
import RorIcon from '@/components/icons/RorIcon'
import Banner from '@/components/identifier/Banner'
import Persist from '@/components/identifier/Persist'
import { formatLanguage } from '@/utils'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    Persist,
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
      loading: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    access () {
      return this.userStore.getAccess.value
    },
    database () {
      return this.cacheStore.getDatabase.value
    },
    pid () {
      return `/pid/${this.database.identifier.id}`
    },
    identifierLang () {
      return this.identifier.language ? formatLanguage(this.identifier.language.toLowerCase()) : null
    },
    hasLicenses () {
      return this.identifier.licenses && this.identifier.licenses.length > 0
    },
    publication () {
      if (this.identifier.publication_year && !this.identifier.publication_month && !this.identifier.publication_day) {
        return this.identifier.publication_year
      } else if (this.identifier.publication_year && this.identifier.publication_month && this.identifier.publication_day) {
        const month = this.identifier.publication_month
        const day = this.identifier.publication_day
        return `${this.identifier.publication_year}-${month < 9 ? '0' + month : month}-${day < 9 ? '0' + day : day}`
      } else {
        return null
      }
    }
  },
  methods: {
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
    }
  }
}
</script>
