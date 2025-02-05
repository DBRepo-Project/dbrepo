<template>
  <div>
    {{ prefix }}: <a :href="href">{{ displayName }}</a>
  </div>
</template>

<script>
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
      const identifierService = useIdentifierService()
      return identifierService.identifierToDisplayAcronym(this.identifier)
    },
    displayName () {
      const identifierService = useIdentifierService()
      return identifierService.identifierToDisplayName(this.identifier)
    },
    href () {
      if (!this.identifier) {
        return null
      }
      const identifierService = useIdentifierService()
      return identifierService.identifierToUrl(this.identifier)
    }
  }
}
</script>
