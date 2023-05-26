<template>
  <div>
    <v-progress-linear v-if="ontologies.length === 0" indeterminate />
    <v-card v-if="!$vuetify.theme.dark && ontologies.length > 0" flat tile>
      <v-divider class="mx-4" />
    </v-card>
    <v-card
      v-for="(ontology, idx) in ontologies"
      :key="idx"
      :to="`/semantic/ontology/${ontology.id}`"
      flat
      tile>
      <v-divider v-if="idx !== 0" class="mx-4" />
      <v-card-title v-text="ontology.prefix" />
      <v-card-subtitle class="db-subtitle" v-text="ontology.uri" />
      <v-card-text class="db-description">
        <div class="db-tags">
          <v-chip v-if="ontology.sparql" small color="green" outlined>SPARQL</v-chip>
          <v-chip v-if="ontology.rdf" small outlined>RDF</v-chip>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    ontologies () {
      return this.$store.state.ontologies
    },
    canDeleteOntology () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('delete-ontology')
    }
  },
  mounted () {
  },
  methods: {
  }
}
</script>

<style>
.v-chip:not(:first-child) {
  margin-left: 8px;
}
.db-subtitle {
  padding-bottom: 8px;
}
.db-tags {
  margin-bottom: 8px;
}
</style>
