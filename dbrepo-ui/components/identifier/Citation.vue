<template>
  <div v-if="identifier">
    <v-list-item-title class="mt-2">
      Citation
    </v-list-item-title>
    <v-list-item-content>
      <v-row no-gutters>
        <v-col v-if="!loading" md="10">
          <pre v-text="citation" />
        </v-col>
        <v-col
          md="2"
          class="hidden-md-and-down cite-style">
          <v-select
            v-model="style"
            :items="styles"
            item-text="style"
            item-value="accept"
            dense
            outlined
            single-line />
        </v-col>
      </v-row>
    </v-list-item-content>
  </div>
</template>

<script>
import IdentifierService from '@/api/identifier.service'

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
      IdentifierService.findAccept(this.identifier.id, accept)
        .then((citation) => {
          this.citation = citation
        })
        .finally(() => {
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
