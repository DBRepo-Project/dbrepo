<template>
  <div v-if="canListOntologies">
    <v-toolbar flat>
      <v-toolbar-title>{{ $t('layout.semantics', { name: 'vue-i18n' }) }}</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canListOntologies" to="/semantic/ontology" color="secondary">
          {{ ontologies.length }} Ontologies
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-tabs v-model="tab">
      <v-tab>Concepts</v-tab>
      <v-tab>Units</v-tab>
    </v-tabs>
    <v-card flat>
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="rows"
          :options.sync="options"
          :server-items-length="total"
          :footer-props="footerProps">
          <template v-slot:item.uri="{ item }">
            <a :href="item.uri" target="_blank" v-text="item.uri" />
          </template>
          <template v-slot:item.action="{ item }">
            <v-btn small :disabled="disabled(item)" @click="view(item)">
              Usages
            </v-btn>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
    <v-dialog
      v-model="viewSemanticEntityDialog"
      max-width="640">
      <ViewSemanticEntity :mode="mode" :entity="entity" @close="close" />
    </v-dialog>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import SemanticService from '@/api/semantic.service'
import ViewSemanticEntity from '@/components/dialogs/ViewSemanticEntity.vue'

export default {
  components: {
    ViewSemanticEntity
  },
  data () {
    return {
      loadingConcepts: false,
      loadingUnits: false,
      entity: null,
      viewSemanticEntityDialog: false,
      headers: [
        { text: 'URI', value: 'uri' },
        { text: 'Name', value: 'name' },
        { text: 'Description', value: 'description' },
        { text: 'Usages', value: 'usages' },
        { text: null, value: 'action' }
      ],
      options: {
        page: 1,
        itemsPerPage: 10
      },
      total: -1,
      footerProps: {
        'items-per-page-options': [10, 20, 30, 40, 50]
      },
      tab: 0,
      tabs: [
        'concepts', 'units'
      ],
      concepts: [],
      units: [],
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
    ontologies () {
      return this.$store.state.ontologies
    },
    rows () {
      return this.tab === 0 ? this.concepts : this.units
    },
    mode () {
      return this.tab === 0 ? 'concept' : 'unit'
    },
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
    }
  },
  mounted () {
    this.loadUnits()
    this.loadConcepts()
  },
  methods: {
    loadConcepts () {
      this.loadingConcepts = true
      SemanticService.findAllConcepts()
        .then((concepts) => {
          concepts = concepts.map((column) => {
            column.usages = column.columns.length
            return column
          })
          this.concepts = concepts
        })
        .catch(() => {
          this.loadingConcepts = false
        })
        .finally(() => {
          this.loadingConcepts = false
        })
    },
    loadUnits () {
      this.loadingUnits = true
      SemanticService.findAllUnits()
        .then((units) => {
          units = units.map((unit) => {
            unit.usages = unit.columns.length
            return unit
          })
          this.units = units
        })
        .catch(() => {
          this.loadingUnits = false
        })
        .finally(() => {
          this.loadingUnits = false
        })
    },
    disabled (item) {
      return !item.usages || this.usages === 0
    },
    view (entity) {
      this.entity = entity
      this.viewSemanticEntityDialog = true
    },
    close (event) {
      if (this.mode === 'unit') {
        this.loadUnits()
      } else if (this.mode === 'concept') {
        this.loadConcepts()
      }
      this.viewSemanticEntityDialog = false
    }
  }
}
</script>
