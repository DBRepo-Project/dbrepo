<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
    <v-toolbar flat>
      <v-toolbar-title>
        <span>Create Table</span>
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
                :rules="[v => notEmpty(v) || $t('Required')]"
                :error-messages="!validTableName ? ['Table with this name exists'] : []"
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
                :rules="[v => notEmpty(v) || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-btn :disabled="!step1Valid || !validTableName" color="primary" type="submit" @click="step = 2">
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
        <TableSchema :back="true" :columns="tableCreate.columns" @close="schemaClose" />
      </v-stepper-content>
    </v-stepper>
  </div>
</template>

<script>
import TableSchema from '@/components/TableSchema'
const { notEmpty } = require('@/utils')
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
      tableNames: [],
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
    validTableName () {
      if (this.tableCreate.name === null) {
        return true
      }
      if (this.tableCreate.name.length < 3) {
        return true
      }
      return !this.tableNames.includes(this.tableCreate.name.toString()
        .normalize('NFKD')
        .toLowerCase()
        .trim()
        .replace(/\s+/g, '-')
        .replace(/[^\w-]+/g, '')
        .replace(/--+/g, '_'))
    }
  },
  mounted () {
    this.listTables()
  },
  methods: {
    notEmpty,
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
        this.$toast.error('Could not create table')
      }
    },
    async listTables () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        console.debug('tables', res.data)
        this.tableNames = res.data.map(t => t.internal_name)
      } catch (err) {
        this.error = true
        console.error('could not list tables', err)
        this.$toast.error('Could not list tables')
      }
      this.loading = false
    },
    schemaClose (event) {
      console.debug('schema closed', event)
      if (!event.success) {
        this.step = 1
        return
      }
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
