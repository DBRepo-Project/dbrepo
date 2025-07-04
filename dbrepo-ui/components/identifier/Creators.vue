<template>
  <div>
    <p>
      <span
        v-for="(personOrOrg, i) in creators"
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
        <span>
          {{ personOrOrg.creator_name }}
        </span>
        <sup
          v-if="hasAffiliation(personOrOrg)"
          class="ml-1">
          {{ personOrOrg.affiliation_index }}
        </sup>
        <span
          v-if="!isLast(creators, i)">;&nbsp;</span>
      </span>
    </p>
    <p class="mt-2">
      <span
        v-for="(affiliation, i) in affiliations"
        :key="`c-${i}`">
        <sup>
          {{ i+1 }}
        </sup>
        {{ affiliation.name }}
        <RorIcon
          v-if="hasRor(affiliation)"
          class="mr-1"
          :ror="affiliation.name_identifier" />
      </span>
    </p>
  </div>
</template>
<script>
import RorIcon from '@/components/icons/RorIcon.vue'
import IsniIcon from '@/components/icons/IsniIcon.vue'
import OrcidIcon from '@/components/icons/OrcidIcon.vue'

export default {
  components: {OrcidIcon, IsniIcon, RorIcon},
  props: {
    personOrOrgs: {
      type: Array,
      default () {
        return []
      }
    }
  },
  data () {
    return {
      creators: [],
      affiliations: []
    }
  },
  mounted() {
    this.personOrOrgs.forEach(personOrOrg => {
      const creator = Object.assign({}, personOrOrg)
      if (this.getIndex(creator) !== -1) {
        creator.affiliation_index = this.getIndex(creator) + 1
        this.creators.push(creator)
        return
      }
      this.creators.push(creator)
      if (!(personOrOrg.affiliation || personOrOrg.affiliation_identifier || personOrOrg.affiliation_identifier_scheme)) {
        return
      }
      this.affiliations.push({
        name: personOrOrg.affiliation,
        name_identifier: personOrOrg.affiliation_identifier,
      })
      creator.affiliation_index = this.getIndex(creator) + 1
    })
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
      return personOrOrg.affiliation_index
    },
    getIndex (personOrOrg) {
      if (!personOrOrg) {
        return null
      }
      return this.affiliations.map(a => a.name).indexOf(personOrOrg.affiliation)
    },
    isLast (array, index) {
      if (!array || array.length === 0) {
        return true
      }
      return index === array.length - 1
    }
  }
}
</script>
