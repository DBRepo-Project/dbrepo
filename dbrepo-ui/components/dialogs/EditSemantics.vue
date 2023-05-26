<template>
  <div>
    <v-card>
      <v-card-title>Edit</v-card-title>
      <v-card-text>
        <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
          <v-row>
            <v-col>
              <v-text-field
                v-model="semanticDto.name"
                clearable
                label="Name" />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                v-model="semanticDto.uri"
                clearable
                label="URI"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          class="mb-2"
          @click="cancel">
          Cancel
        </v-btn>
        <v-btn
          color="primary"
          class="mb-2 mr-2"
          :disabled="!valid"
          :loading="loadingSave"
          @click="save">
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import SemanticService from '@/api/semantic.service'

export default {
  props: {
    entity: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      valid: false,
      localEntity: null,
      loadingSave: false,
      semanticDto: {
        name: null,
        uri: null
      }
    }
  },
  computed: {
  },
  watch: {
    entity () {
      this.semanticDto.name = this.entity.name
      this.semanticDto.uri = this.entity.uri
    }
  },
  mounted () {
    this.semanticDto.name = this.entity.name
    this.semanticDto.uri = this.entity.uri
  },
  methods: {
    cancel () {
      this.$emit('close', { success: false, action: 'cancel' })
    },
    save () {
      SemanticService.u
    },
    submit () {
      this.$refs.form.validate()
    }
  }
}
</script>
<style scoped>
</style>
