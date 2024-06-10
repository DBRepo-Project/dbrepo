<template>
  <div v-if="canListOntologies">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" plain class="mr-2" to="/semantic/ontology">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
        <span v-if="!loading">
          Ontology <a v-if="ontology" :href="ontology.uri" target="_blank" v-text="ontology.uri" />
        </span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canDeleteOntology" :loading="loadingDelete" color="error" @click="deleteOntology">
          Delete Ontology
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card v-if="ontology" variant="flat">
        <v-card-text>
          <v-row dense>
            <v-col cols="6">
              <v-text-field
                id="prefix"
                v-model="ontologyChangeDto.prefix"
                name="prefix"
                label="Prefix *"
                hint="Only lowercase alphanumeric letters, max. 8"
                autofocus
                :rules="[
                  v => notEmpty(v) || $t('validation.required'),
                  v => validPrefix(v) || $t('validation.prefix.pattern'),
                  v => validPrefixLength(v,1,8) || $t('validation.prefix.length'),
                  v => validPrefixNotExists(v) || $t('validation.prefix.exists')
                ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-text-field
                id="uri"
                v-model="ontologyChangeDto.uri"
                name="uri"
                label="URI *"
                :rules="[
                  v => notEmpty(v) || $t('validation.required'),
                  v => validUri(v) || $t('validation.uri.pattern'),
                  v => validUriNotExists(v) || $t('validation.uri.exists')
                ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-text-field
                id="sparql-endpoint"
                v-model="ontologyChangeDto.sparql_endpoint"
                name="sparql-endpoint"
                label="SPARQL Endpoint"
                :rules="[
                  v => validUriOptional(v) || $t('validation.uri.pattern')
                ]" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            id="createDB"
            class="mb-2 ml-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            :loading="loading"
            @click="save">
            Update
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import { notEmpty } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  data () {
    return {
      loading: false,
      loadingDelete: false,
      ontology: null,
      ontologyChangeDto: {
        uri: null,
        prefix: null,
        sparql_endpoint: null
      },
      valid: false,
      createOntologyDialog: false,
      items: [
        { text: `${this.$t('layout.semantics')}`, to: '/semantic', activeClass: '' },
        { text: `${this.$t('layout.ontologies')}`, to: '/semantic/ontology', activeClass: '' },
        { text: `${this.$route.params.ontology_id}`, to: `/semantic/ontology/${this.$route.params.ontology_id}`, activeClass: '' }
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
    canListOntologies () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('list-ontologies')
    },
    canDeleteOntology () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('delete-ontology')
    }
  },
  mounted () {
    this.loadOntology()
  },
  methods: {
    loadOntology () {
      this.loading = true
      const ontologyService = useOntologyService()
      ontologyService.findOne(this.$route.params.ontology_id)
        .then((ontology) => {
          this.ontology = ontology
          this.ontologyChangeDto = Object.assign({}, ontology)
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    deleteOntology () {
      this.loadingDelete = true
      const ontologyService = useOntologyService()
      ontologyService.remove(this.$route.params.ontology_id)
        .then(async () => {
          // await this.$store.dispatch('reloadOntologies')
          await this.$router.push('/semantic/ontology')
        })
        .catch(() => {
          this.loadingDelete = false
        })
        .finally(() => {
          this.loadingDelete = false
        })
    },
    save () {
      this.loading = true
      const payload = {
        uri: this.ontologyChangeDto.uri,
        prefix: this.ontologyChangeDto.prefix,
        sparql_endpoint: this.ontologyChangeDto.sparql_endpoint
      }
      const ontologyService = useOntologyService()
      ontologyService.update(this.$route.params.ontology_id, payload)
        .then(() => {
          this.loadOntology()
          // this.$store.dispatch('reloadOntologies')
          const toast = useToastInstance()
          toast.success('Successfully update ontology!')
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    validPrefix (str) {
      if (!str) {
        return false
      }
      return str.match(/[a-z0-9]+/g)
    },
    validPrefixLength (str, min, max) {
      if (!str) {
        return false
      }
      return str.length > min && str.length <= max
    },
    validPrefixNotExists (str) {
      const ontologies = this.ontologies.filter(o => o.prefix === str)
      if (ontologies && ontologies.length !== 0) {
        /* same prefix is fine for the same ontology, but not for others */
        return ontologies[0].id === this.ontology.id
      }
      return !this.ontologies.map(o => o.prefix).includes(str)
    },
    validUriNotExists (str) {
      const ontologies = this.ontologies.filter(o => o.uri === str)
      if (ontologies && ontologies.length !== 0) {
        /* same uri is fine for the same ontology, but not for others */
        return ontologies[0].id === this.ontology.id
      }
      return !this.ontologies.map(o => o.uri).includes(str)
    },
    validUriOptional (str) {
      if (!str) {
        return true
      }
      return this.validUri(str)
    },
    validUri (str) {
      if (!str) {
        return false
      }
      return str.match(/^https?:\/\//g)
    },
    close (event) {
      if (event.success) {
        // this.$store.dispatch('reloadOntologies')
      }
      this.createOntologyDialog = false
    },
    submit () {
      this.$refs.form.validate()
    },
    notEmpty
  }
}
</script>
<style>
.skeleton-medium > div {
  width: 200px !important;
}
.skeleton-xsmall > div {
  width: 50px !important;
}
</style>
