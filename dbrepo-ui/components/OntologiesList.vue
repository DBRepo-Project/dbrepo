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

<script>
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  data () {
    return {
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    token () {
      return this.userStore.getToken
    },
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    ontologies () {
      return this.cacheStore.getOntologies
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
