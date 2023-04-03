<template>
  <p v-if="identifier?.doi">DOI: <a :href="doiUrl(identifier.doi)">{{ identifier.doi }}</a></p>
  <p v-else-if="identifier?.id"><a :href="pidUrl(identifier.id)">{{ pidUrl(identifier.id) }}</a></p>
</template>
yarn dev
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
      return `${location.protocol}//${location.host}`
    }
  },
  methods: {
    pidUrl (pid) {
      return `${this.baseUrl}/pid/${pid}`
    },
    doiUrl (doi) {
      return `${this.$config.doiUrl}/${doi}`
    }
  }
}
</script>

<style scoped></style>
