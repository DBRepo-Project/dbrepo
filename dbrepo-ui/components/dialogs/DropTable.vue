<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-card-title>Drop table {{ table.internal_name }}</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col>
              This action cannot be undone! Type the table name <code>{{ table.internal_name }}</code> below if you really want to drop it with all stored data.
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="confirm"
                v-model="confirm"
                name="confirm"
                label="Table Name *"
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
            :disabled="confirm !== table.internal_name"
            @click="dropTable">
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import TableService from '@/api/table.service'

export default {
  data () {
    return {
      confirm: null,
      loadingDelete: false,
      valid: false
    }
  },
  computed: {
    table () {
      return this.$store.state.table
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { action: 'closed' })
    },
    dropTable () {
      if (!this.table.id) {
        return
      }
      this.loadingDelete = true
      TableService.delete(this.table.database.id, this.table.id)
        .then(() => {
          console.info('Deleted table with id ', this.table.id)
          this.$toast.success('Successfully deleted table with id ' + this.table.id)
          this.$emit('close', { action: 'deleted' })
        })
        .finally(() => {
          this.loadingDelete = false
        })
    }
  }
}
</script>
