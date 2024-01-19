<template>
  <div>
    {{ prefix }}: <a :href="href">{{ displayName }}</a>
  </div>
</template>
<script>
import IdentifierMapper from '@/api/identifier.mapper'

export default {
  props: {
    identifier: {
      type: Object,
      default () {
        return null
      }
    }
  },
  computed: {
    prefix () {
      return IdentifierMapper.identifierToDisplayAcronym(this.identifier)
    },
    displayName () {
      return IdentifierMapper.identifierToDisplayName(this.identifier)
    },
    href () {
      return IdentifierMapper.identifierToUrl(this.identifier)
    }
  }
}
</script>
