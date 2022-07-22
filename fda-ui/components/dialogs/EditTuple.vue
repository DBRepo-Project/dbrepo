<template>
  <div v-if="tuple">
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title>
          Update Tuple
        </v-card-title>
        <v-card-text>
          <div v-for="(attr,idx) in columns" :key="idx">
            <v-text-field
              v-if="attr.column_type === 'number'"
              v-model.number="tuple[attr.internal_name]"
              :disabled="(edit && attr.is_primary_key) || (!edit && attr.auto_generated)"
              class="mb-2"
              :hint="hint(attr)"
              persistent-hint
              :required="!attr.is_null_allowed"
              :label="attr.name"
              type="number" />
            <v-text-field
              v-if="attr.column_type === 'string' || attr.column_type === 'text' || attr.column_type === 'decimal'"
              v-model="tuple[attr.internal_name]"
              :disabled="(edit && attr.is_primary_key) || (!edit && attr.auto_generated)"
              class="mb-2"
              :required="!attr.is_null_allowed"
              :label="attr.name"
              type="text" />
            <v-text-field
              v-if="attr.column_type === 'timestamp'"
              v-model="tuple[attr.internal_name]"
              suffix="UTC"
              hint="e.g. 2022-07-12 18:32:59"
              :rules="[v => /^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$/.test(v) || $t('Required format yyyy-MM-dd HH:mm:ss')]"
              class="mb-2"
              :required="!attr.is_null_allowed"
              :label="attr.name"
              type="text" />
            <v-text-field
              v-if="attr.column_type === 'date'"
              v-model="tuple[attr.internal_name]"
              suffix="UTC"
              hint="e.g. 2022-07-12"
              :rules="[v => /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(v) || $t('Required format yyyy-MM-dd')]"
              class="mb-2"
              :required="!attr.is_null_allowed"
              :label="attr.name"
              type="text" />
            <v-select
              v-if="attr.column_type === 'ENUM'"
              v-model="tuple[attr.internal_name]"
              class="mb-2"
              :required="!attr.is_null_allowed"
              :items="attr.enum_values"
              :label="attr.name" />
            <v-checkbox
              v-if="attr.column_type === 'boolean'"
              v-model="tuple[attr.internal_name]"
              class="mb-2"
              :label="attr.name" />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            v-if="!edit"
            id="addTuple"
            class="mb-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            @click="addTuple">
            Create
          </v-btn>
          <v-btn
            v-if="edit"
            id="updateTuple"
            class="mb-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            @click="updateTuple">
            Update
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    tuple: {
      type: Object,
      default: null
    },
    edit: Boolean
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      columns: this.$parent.$parent.$parent.$parent.table.columns
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$parent.$parent.$parent.$parent.selection = []
      this.$parent.$parent.$parent.$parent.edit = false
      this.$parent.$parent.$parent.$parent.editTupleDialog = false
    },
    hint (attr) {
      if (!this.edit && attr.auto_generated) {
        return 'Value is auto-generated'
      }
      if (this.edit && attr.is_primary_key) {
        return 'Primary key not editable'
      }
      return null
    },
    async updateTuple () {
      const constraints = {}
      this.columns
        .filter(c => c.is_primary_key)
        .forEach((c) => {
          constraints[c.internal_name] = this.tuple[c.internal_name]
        })
      const data = {
        data: this.tuple,
        keys: constraints
      }
      try {
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data`, data, {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        console.info('update result', res.data)
        this.$toast.success('Successfully updated tuple!')
        this.cancel()
      } catch (err) {
        console.error('Failed to update tuple', err)
        this.$toast.error('Failed to update tuple')
      }
    },
    async addTuple () {
      const constraints = {}
      this.columns
        .filter(c => c.is_primary_key)
        .forEach((c) => {
          constraints[c.internal_name] = this.tuple[c.internal_name]
        })
      try {
        const res = await this.$axios.post(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data`, {
          data: this.tuple
        }, {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        console.info('add result', res.data)
        this.$toast.success('Successfully added tuple!')
        this.$parent.$parent.$parent.$parent.loadData()
        this.cancel()
      } catch (err) {
        console.error('Failed to add tuple', err)
        this.$toast.error('Failed to add tuple')
      }
    }
  }
}
</script>
