<template>
  <div>
    {{ prefix }}: <a :href="pidUrl">{{ displayName }}</a>
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
    baseUrl () {
      return `${this.$config.baseUrl}`
    },
    baseDoi () {
      return this.$config.doiUrl
    },
    isDoi () {
      if (!this.identifier) {
        return null
      }
      return this.identifier.doi !== null
    },
    prefix () {
      if (!this.identifier) {
        return null
      }
      return this.isDoi ? 'DOI' : 'URI'
    },
    pidUrl () {
      if (!this.identifier) {
        return null
      }
      return this.isDoi ? `${this.baseDoi}/${this.identifier.doi}` : `${this.baseUrl}/pid/${this.identifier.id}`
    },
    displayName () {
      if (!this.identifier) {
        return null
      }
      return this.isDoi ? `${this.identifier.doi}` : `/pid/${this.identifier.id}`
    }
  }
}
</script>
