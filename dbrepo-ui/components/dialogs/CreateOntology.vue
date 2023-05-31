<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-card-title>Create Ontology</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-text-field
                id="prefix"
                v-model="createOntologyDto.prefix"
                name="prefix"
                label="Prefix *"
                hint="Only lowercase alphanumeric letters, max. 8"
                autofocus
                :rules="[
                  v => notEmpty(v) || $t('Required'),
                  v => validPrefix(v) || $t('Invalid prefix pattern'),
                  v => validPrefixLength(v,1,8) || $t('Invalid length: min. 1, max. 8'),
                  v => !ontologies.map(o => o.prefix).includes(v) || $t('Prefix exists')
                ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="uri"
                v-model="createOntologyDto.uri"
                name="uri"
                label="URI *"
                :rules="[
                  v => notEmpty(v) || $t('Required'),
                  v => validUri(v) || $t('Invalid URI'),
                  v => !ontologies.map(o => o.uri).includes(v) || $t('URI exists')
                ]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="sparql-endpoint"
                v-model="createOntologyDto.sparql_endpoint"
                name="sparql-endpoint"
                label="SPARQL Endpoint"
                :rules="[
                  v => validUriOptional(v) || $t('Invalid URL')
                ]" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            id="createDB"
            class="mb-2 mr-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            :loading="loading"
            @click="create">
            Create
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { notEmpty } from '@/utils'
import SemanticService from '@/api/semantic.service'

export default {
  data () {
    return {
      valid: false,
      loading: false,
      createOntologyDto: {
        uri: null,
        prefix: null,
        sparql_endpoint: null
      }
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    ontologies () {
      return this.$store.state.ontologies
    }
  },
  mounted () {
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
    create () {
      this.loading = true
      SemanticService.registerOntology(this.createOntologyDto)
        .then((ontology) => {
          this.$emit('close', { success: true })
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
    notEmpty
  }
}
</script>
