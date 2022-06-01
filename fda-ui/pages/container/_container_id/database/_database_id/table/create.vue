<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
    <v-toolbar flat>
      <v-toolbar-title>
        <span>Create Table</span>
      </v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn :disabled="!canCreateTable" color="primary" @click="createTable">
          Create Table
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-stepper v-model="step" vertical flat>
      <v-stepper-step :complete="step > 1" step="1">
        Table Information
      </v-stepper-step>

      <v-stepper-content step="1">
        <v-form ref="form" v-model="valid" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableCreate.name"
                name="name"
                label="Table Name *"
                autocomplete="off"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableCreate.description"
                name="description"
                label="Description *"
                autocomplete="off"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-btn :disabled="!step1Valid" color="primary" type="submit" @click="step = 2">
                Continue
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-stepper-content>

      <v-stepper-step :complete="step > 2" step="2">
        Table Schema
      </v-stepper-step>

      <v-stepper-content step="2">
        <TableSchema :form="valid" :columns="tableCreate.columns" @close="schemaClose" />
      </v-stepper-content>
    </v-stepper>
  </div>
</template>

<script>
import TableSchema from '@/components/TableSchema'
export default {
  components: {
    TableSchema
  },
  data () {
    return {
      columns: [],
      name: null,
      valid: false,
      description: null,
      loading: false,
      step: 1,
      error: false,
      tableCreate: {
        name: null,
        description: null,
        columns: []
      }
    }
  },
  computed: {
    databaseId () {
      return this.$route.params.database_id
    },
    step1Valid () {
      return this.tableCreate.name !== null && this.tableCreate.name.length > 0 && this.tableCreate.description !== null && this.tableCreate.description.length > 0
    },
    token () {
      return this.$store.state.token
    },
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    canCreateTable () {
      if (!this.step1Valid || this.step !== 2) {
        return false
      }
      return this.tableCreate.columns.length >= 1
    }
  },
  mounted () {
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    async createTable () {
      try {
        this.loading = true
        const res = await this.$axios.post(`/api/container/${this.$route.params.container_id}/database/${this.databaseId}/table`, this.tableCreate, {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        if (res.status === 201) {
          this.error = false
          this.$toast.success('Table created.')
          this.$root.$emit('table-create', res.data)
          await this.$router.push(`/container/${this.$route.params.container_id}/database/${this.databaseId}/table/${res.data.id}`)
        } else {
          this.error = true
          this.$toast.error(`Could not create table: status ${res.status}`)
        }
      } catch (err) {
        this.error = true
        console.error('could not create table', err)
        this.$toast.error('Could not create table.')
      }
    },
    schemaClose (event) {
      console.trace('schema closed', event)
      this.createTable()
    }
  }
}
</script>

<style>
.row-border {
  border: 1px solid #ccc;
  border-radius: 3px;
  margin: 0 !important;
}
</style>
