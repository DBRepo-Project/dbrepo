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
            <span v-text="title.title" />
          </p>
        </v-list-item>
        <v-list-item
          :title="$t('pages.identifier.descriptions.title')"
          density="compact">
          <p
            v-for="(description, i) in identifier.descriptions"
            :key="`d-${i}`">
            <div
              v-text="description?.type"
              class="text-subtitle-2" />
            <span v-text="description.description" />
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
          <Creators :person-or-orgs="identifier.creators" />
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
            <Banner
              :identifier="related" />
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
          v-if="canCitation"
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
import Citation from '@/components/identifier/Citation.vue'
import IsniIcon from '@/components/icons/IsniIcon.vue'
import OrcidIcon from '@/components/icons/OrcidIcon.vue'
import RorIcon from '@/components/icons/RorIcon.vue'
import Banner from '@/components/identifier/Banner.vue'
import Persist from '@/components/identifier/Persist.vue'
import Creators from '@/components/identifier/Creators.vue'
import { formatLanguage } from '@/utils'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    Persist,
    Citation,
    IsniIcon,
    OrcidIcon,
    RorIcon,
    Banner,
    Creators
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
    },
    canCitation () {
      return this.identifier && this.identifier.status === 'published'
    }
  }
}
</script>
