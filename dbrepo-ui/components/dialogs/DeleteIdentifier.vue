<template>
  <div>
    <v-card>
      <v-card-title v-text="title" />
      <v-card-text>
        <v-row dense>
          <v-col>
            This action cannot be undone! Type the identifier <strong>{{ confirmText }}</strong> below if you really want to delete it.
          </v-col>
        </v-row>
        <v-row dense>
          <v-col>
            <v-text-field
              id="confirm"
              v-model="confirm"
              name="confirm"
              label="Identifier *"
              autofocus
              required />
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
          class="mb-2 mr-1"
          color="error"
          :loading="loadingDelete"
          :disabled="confirm !== confirmText"
          @click="deleteIdentifier">
          Delete
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import IdentifierService from '@/api/identifier.service'

export default {
  props: {
    identifier: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      confirm: null,
      loadingDelete: false
    }
  },
  computed: {
    title () {
      if (this.identifier.doi) {
        return `DOI ${this.identifier.doi}`
      }
      return `Identifier with id ${this.identifier.id}`
    },
    confirmText () {
      if (this.identifier.doi) {
        return this.identifier.doi
      }
      return `/pid/${this.identifier.id}`
    }
  },
  methods: {
    cancel () {
      this.$parent.$parent.$parent.persistQueryDialog = false
      this.$emit('close', { action: 'closed' })
    },
    deleteIdentifier () {
      if (!this.identifier.id) {
        return
      }
      this.loadingDelete = true
      IdentifierService.delete(this.identifier.id)
        .then(() => {
          console.info('Deleted identifier with id ', this.identifier.id)
          this.$toast.success('Successfully deleted identifier with id ' + this.identifier.id)
          this.$emit('close', { action: 'deleted' })
        })
        .finally(() => {
          this.loadingDelete = false
        })
    }
  }
}
</script>
