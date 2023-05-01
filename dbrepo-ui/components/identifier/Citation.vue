<template>
  <div v-if="pid">
    <v-list-item-title class="mt-2">
      Citation
    </v-list-item-title>
    <v-list-item-content>
      <v-row no-gutters>
        <v-col
          v-if="loading">
          <v-skeleton-loader
            class="skeleton-large"
            type="list-item-two-line" />
        </v-col>
        <v-col
          v-if="!loading"
          md="10">
          <pre
            v-text="citation" />
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
    pid: {
      type: Number,
      default () {
        return null
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
  computed: {
    config () {
      return {
        headers: { Accept: 'text/bibliography;style=apa' },
        progress: false
      }
    }
  },
  watch: {
    style (newVal, _) {
      this.loadCitation(newVal)
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
    async loadCitation (accept) {
      if (!this.pid) {
        return
      }
      this.loading = true
      try {
        const config = this.config
        if (accept != null) {
          config.headers.Accept = accept
        }
        const res = await this.$axios.get(`/api/pid/${this.pid}`, config)
        this.citation = res.data
        console.debug('citation', this.citation)
      } catch (err) {
        console.error('Could not cite identifier', err)
        this.$toast.error('Could not cite identifier')
        this.error = true
      }
      this.loading = false
    }
  }
}
</script>
<style scoped>
.cite-style {
  cursor: pointer !important;
}
</style>
