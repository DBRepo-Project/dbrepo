<template>
  <div v-if="canListOntologies">
    <v-toolbar flat>
      <v-toolbar-title v-text="$t('pages.semantics.title')" />
      <v-spacer />
      <v-btn
        v-if="canListOntologies"
        to="/semantic/ontology"
        variant="flat"
        :text="ontologies.length + ' ' + $t('toolbars.semantic.ontologies.text')"
        color="secondary" />
      <template v-slot:extension>
        <v-tabs
          v-model="tab"
          color="primary">
          <v-tab
            v-text="$t('toolbars.semantic.ontologies.concepts')" />
          <v-tab
            v-text="$t('toolbars.semantic.ontologies.units')" />
        </v-tabs>
      </template>
    </v-toolbar>
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
            <v-btn
              small
              :disabled="disabled(item)"
              :text="$t('pages.semantics.usages.text')"
              @click="view(item)" />
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
    <v-dialog
      v-model="viewSemanticEntityDialog"
      max-width="640">
      <ViewSemanticEntity
        :mode="mode"
        :entity="entity"
        @close="close" />
    </v-dialog>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script>
import ViewSemanticEntity from '@/components/dialogs/ViewSemanticEntity'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

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
        {
          title: `${this.$t('navigation.semantics')}`,
          to: '/semantic'
        }
      ],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    ontologies () {
      return this.cacheStore.getOntologies
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
      const conceptService = useConceptService()
      conceptService.findAll()
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
      const unitService = useUnitService()
      unitService.findAll()
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
