<template>
  <div>
    <v-card
      v-for="(ontology, idx) in ontologies"
      :key="idx"
      :to="`/semantic/ontology/${ontology.id}`"
      variant="flat"
      rounded="0">
      <v-divider
        class="mx-4" />
      <v-card-title>
        {{ ontology.prefix }}
      </v-card-title>
      <v-card-subtitle>
        {{ ontology.uri }}
      </v-card-subtitle>
      <v-card-text>
        <div
          class="db-tags">
          <v-chip
            v-if="ontology.sparql"
            size="small"
            color="success"
            text="SPARQL"
            variant="outlined" />
          <v-chip
            v-if="ontology.rdf"
            size="small"
            text="RDF"
            variant="outlined" />
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
</script>
<script>
import { useCacheStore } from '@/stores/cache.js'

export default {
  data () {
    return {
      cacheStore: useCacheStore()
    }
  },
  computed: {
    ontologies () {
      return []
    }
  },
  mounted () {
  },
  methods: {
  }
}
</script>

<style>
.db-tags .v-chip:not(:first-child) {
  margin-left: 4px;
}
</style>
