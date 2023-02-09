<template>
  <div v-if="tuple">
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title v-text="title" />
        <v-card-text>
          <div v-for="(attr,idx) in columns" :key="idx">
            <v-text-field
              v-if="attr.column_type === 'number'"
              v-model.number="tuple[attr.internal_name]"
              :disabled="(!edit && attr.auto_generated)"
              class="mb-2"
              :hint="hint(attr)"
              persistent-hint
              :rules="attr.is_null_allowed ? [] : [ v => !!v || $t('Required') ]"
              :required="required(attr)"
              :label="label(attr)"
              type="number" />
            <v-text-field
              v-if="attr.column_type === 'string' || attr.column_type === 'text' || attr.column_type === 'decimal'"
              v-model="tuple[attr.internal_name]"
              :disabled="(edit && attr.is_primary_key) || (!edit && attr.auto_generated)"
              class="mb-2"
              :rules="attr.is_null_allowed ? [] : [ v => !!v || $t('Required') ]"
              :required="required(attr)"
              :label="label(attr)"
              type="text" />
            <v-text-field
              v-if="attr.column_type === 'timestamp'"
              v-model="tuple[attr.internal_name]"
              suffix="UTC"
              hint="e.g. 2022-07-12 18:32:59"
              :rules="attr.is_null_allowed ? [ validateTimestamp ] : [validateTimestamp || $t('Required format yyyy-MM-dd HH:mm:ss'), v => !!v || $t('Required')]"
              class="mb-2"
              :required="required(attr)"
              :label="label(attr)"
              type="text" />
            <v-menu
              v-if="attr.column_type === 'date'"
              ref="menu"
              v-model="menu"
              :close-on-content-click="true"
              transition="scale-transition"
              offset-y
              min-width="auto">
              <template v-slot:activator="{ on, attrs }">
                <v-text-field
                  v-model="tuple[attr.internal_name]"
                  :label="label(attr)"
                  suffix="UTC"
                  readonly
                  v-bind="attrs"
                  v-on="on" />
              </template>
              <v-date-picker
                v-model="tuple[attr.internal_name]"
                color="primary"
                no-title
                scrollable />
            </v-menu>
            <v-select
              v-if="attr.column_type === 'ENUM'"
              v-model="tuple[attr.internal_name]"
              class="mb-2"
              :rules="attr.is_null_allowed ? [] : [ v => !!v || $t('Required') ]"
              :required="required(attr)"
              :items="attr.enum_values"
              :label="label(attr)" />
            <v-checkbox
              v-if="attr.column_type === 'boolean'"
              v-model="tuple[attr.internal_name]"
              :rules="attr.is_null_allowed ? [] : [ v => !!v || $t('Required') ]"
              :required="required(attr)"
              class="mb-2"
              :label="label(attr)" />
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
            :disabled="!valid"
            color="primary"
            type="submit"
            @click="addTuple">
            Create
          </v-btn>
          <v-btn
            v-if="edit"
            id="updateTuple"
            class="mb-2"
            :disabled="!valid"
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
    edit: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      menu: false,
      columns: this.$parent.$parent.$parent.$parent.table.columns
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    title () {
      return (this.edit ? 'Edit' : 'Add') + ' tuple'
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.menu = false
      this.$emit('close', { success: false })
    },
    hint (attr) {
      if (!this.edit && attr.auto_generated) {
        return 'Value is auto-generated'
      }
      if (this.edit && attr.is_primary_key) {
        return 'Required (Primary Key)'
      }
      if (!attr.is_null_allowed) {
        return 'Required'
      }
      return null
    },
    label (attr) {
      return attr.name + (!attr.is_null_allowed ? ' *' : '')
    },
    required (attr) {
      return attr.is_null_allowed
    },
    validateTimestamp (val) {
      return /^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$/.test(val)
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
        await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/data`, data, {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        console.info('update result')
        this.$toast.success('Successfully updated tuple!')
        this.$emit('close', { success: true })
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
        this.$emit('close', { success: true })
      } catch (error) {
        console.error('Failed to add tuple', error)
        const { message, status } = error.response.data
        if (status === 423) {
          console.error('Database failed to accept tuple', message)
          this.$toast.error(`Database failed to accept tuple: ${message}`)
        } else {
          console.error('Failed to add tuple', message)
          this.$toast.error(`${message}`)
        }
      }
    }
  }
}
</script>
