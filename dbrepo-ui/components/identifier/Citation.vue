<template>
  <v-row
    no-gutters>
    <v-col
      v-if="!loading"
      md="10">
      {{ citation }}
    </v-col>
    <v-col
      v-if="!$vuetify.display.mdAndDown"
      md="2"
      class="cite-style">
      <v-select
        v-model="style"
        :items="styles"
        item-title="title"
        item-value="value"
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
        { title: 'APA', value: 'text/bibliography;style=apa' },
        { title: 'IEEE', value: 'text/bibliography;style=ieee' },
        { title: 'BibTeX', value: 'text/bibliography;style=bibtex' }
      ],
      style: 'text/bibliography;style=apa',
      citation: null
    }
  },
  watch: {
    style () {
      this.loadCitation()
    },
    pid () {
      this.loadCitation()
    }
  },
  mounted () {
    this.loadCitation()
  },
  methods: {
    loadCitation () {
      if (!this.identifier || !this.style) {
        return
      }
      this.loading = true
      const identifierService = useIdentifierService()
      identifierService.findOne(this.identifier.id, this.style)
        .then((citation) => {
          this.citation = citation
          this.loading = false
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(`${code}: ${message}`))
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
