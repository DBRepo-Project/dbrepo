<template>
  <div v-if="canListOntologies">
    <v-toolbar flat>
      <v-btn
        variant="plain"
        size="small"
        icon="mdi-arrow-left"
        to="/semantic" />
      <v-toolbar-title
        v-text="ontologies.length + ' ' + $t('toolbars.semantic.ontologies.title')" />
      <v-spacer />
      <v-btn
        v-if="canCreateOntology"
        color="secondary"
        variant="flat"
        name="create-ontology"
        prepend-icon="mdi-plus"
        :text="$t('toolbars.semantic.ontology.text')"
        @click.stop="createOntologyDialog = true" />
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
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    OntologiesList,
    CreateOntology
  },
  data () {
    return {
      createOntologyDialog: false,
      items: [
        {
          title: `${this.$t('navigation.semantics')}`,
          to: '/semantic'
        },
        {
          title: `${this.$t('navigation.ontologies')}`,
          to: '/semantic/ontology'
        }
      ],
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
        // this.$store.dispatch('reloadOntologies')
      }
      this.createOntologyDialog = false
    }
  }
}
</script>
