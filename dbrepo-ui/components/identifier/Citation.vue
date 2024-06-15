<template>
  <v-row no-gutters>
    <v-col v-if="!loading" md="10">
      <pre v-text="citation" />
    </v-col>
    <v-col
      v-if="!$vuetify.display.mdAndDown"
      md="2"
      class="cite-style">
      <v-select
        v-model="style"
        :items="styles"
        item-title="style"
        item-value="accept"
        dense
        variant="outlined"
        single-line />
    </v-col>
  </v-row>
</template>

<script>
export default {
  props: {
    identifier: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      loading: false,
      styles: [
        { style: 'APA', accept: 'text/bibliography;style=apa' },
        { style: 'IEEE', accept: 'text/bibliography;style=ieee' },
        { style: 'BibTeX', accept: 'text/bibliography;style=bibtex' }
      ],
      style: null,
      citation: null
    }
  },
  watch: {
    style () {
      this.loadCitation(this.style)
    },
    pid () {
      this.loadCitation(this.style)
    }
  },
  mounted () {
    this.style = this.styles[0].accept
    this.loadCitation(null)
  },
  methods: {
    loadCitation (accept) {
      if (!this.identifier || !accept) {
        return
      }
      this.loading = true
      const identifierService = useIdentifierService()
      identifierService.findOne(this.identifier.id, accept)
        .then((citation) => {
          this.citation = citation
          this.loading = false
        })
        .error(({code, message}) => {
          const toast = useToastInstance()
          toast.error(this.$t(`${code}: ${message}`))
          this.loading = false
        })
    }
  }
}
</script>
<style scoped>
.cite-style {
  cursor: pointer !important;
}
</style>
