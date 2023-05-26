<template>
  <div v-if="canListOntologies">
    <v-toolbar flat>
      <v-toolbar-title>{{ ontologies.length }} {{ $t('layout.ontologies', { name: 'vue-i18n' }) }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canCreateOntology" color="primary" name="create-ontology" @click.stop="createOntologyDialog = true">
          <v-icon left>mdi-plus</v-icon> Ontology
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <OntologiesList />
    <v-dialog
      v-model="createOntologyDialog"
      persistent
      max-width="640">
      <CreateOntology ref="ont" @close="close" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import OntologiesList from '@/components/OntologiesList.vue'
import CreateOntology from '@/components/dialogs/CreateOntology.vue'

export default {
  components: {
    OntologiesList,
    CreateOntology
  },
  data () {
    return {
      createOntologyDialog: false,
      items: [
        { text: `${this.$t('layout.semantics', { name: 'vue-i18n' })}`, to: '/semantic', activeClass: '' },
        { text: `${this.$t('layout.ontologies', { name: 'vue-i18n' })}`, to: '/semantic/ontology', activeClass: '' }
      ]
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
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
    },
    canCreateOntology () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-ontology')
    }
  },
  methods: {
    close (event) {
      if (event.success) {
        this.$store.dispatch('reloadOntologies')
      }
      this.createOntologyDialog = false
    }
  }
}
</script>
