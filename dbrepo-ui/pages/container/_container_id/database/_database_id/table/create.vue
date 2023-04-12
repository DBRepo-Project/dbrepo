<template>
  <div v-if="canCreateTable">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>Create Table</v-toolbar-title>
    </v-toolbar>
    <v-stepper v-model="step" vertical flat tile>
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
              <v-textarea
                v-model="tableCreate.description"
                name="description"
                label="Description *"
                autocomplete="off"
                rows="3"
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
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import TableSchema from '@/components/TableSchema'
import { notEmpty } from '@/utils'
import TableService from '@/api/table.service'

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
      },
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        { text: 'Tables', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, activeClass: '' }
      ]
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
    roles () {
      return this.$store.state.roles
    },
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    canCreateTable () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-table')
    },
    config () {
      if (this.token === null) {
        return { headers: {} }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
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
      return !this.database.tables.map(t => t.internal_name).includes(this.tableCreate.name.toString()
        .normalize('NFKD')
        .toLowerCase()
        .trim()
        .replace(/\s+/g, '-')
        .replace(/[^\w-]+/g, '')
        .replace(/--+/g, '_'))
    }
  },
  mounted () {
  },
  methods: {
    notEmpty,
    submit () {
      this.$refs.form.validate()
    },
    createTable () {
      this.loading = true
      TableService.create(this.$route.params.container_id, this.$route.params.database_id, this.tableCreate)
        .then(async (table) => {
          this.$toast.success('Table created')
          await this.$store.dispatch('reloadDatabase')
          await this.$router.push(`/container/${this.$route.params.container_id}/database/${this.databaseId}/table/${table.id}`)
        })
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
