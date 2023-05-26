<template>
  <div v-if="canListOntologies">
    <v-toolbar flat>
      <v-toolbar-title>{{ $t('layout.semantics', { name: 'vue-i18n' }) }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canListOntologies" to="/semantic/ontology" color="secondary">
          Ontologies
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
export default {
  components: {
  },
  data () {
    return {
      createOntologyDialog: false,
      items: [
        { text: `${this.$t('layout.semantics', { name: 'vue-i18n' })}`, to: '/semantic', activeClass: '' }
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
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
    }
  },
  methods: {
  }
}
</script>
