<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>Create Table Schema (and Import Data) from .csv/.tsv</v-toolbar-title>
    </v-toolbar>
    <v-stepper v-model="step" vertical flat>
      <v-stepper-step :complete="step > 1" step="1">
        Table Information
      </v-stepper-step>

      <v-stepper-content step="1">
        <v-form ref="form" v-model="validStep1" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableCreate.name"
                :rules="[v => notEmpty(v) || $t('Required')]"
                :error-messages="!validTableName ? ['Table with this name exists!'] : []"
                autocomplete="off"
                label="Name *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableCreate.description"
                :rules="[v => notEmpty(v) || $t('Required')]"
                autocomplete="off"
                label="Description *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-btn :disabled="!validStep1" class="mb-1" color="primary" type="submit" @click="step = 2">
                Continue
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-stepper-content>

      <v-stepper-step :complete="step > 2" step="2">
        Metadata
      </v-stepper-step>

      <v-stepper-content step="2">
        <v-form ref="form" v-model="validStep2" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="8">
              <v-select
                v-model="tableImport.separator"
                :items="separators"
                item-text="key"
                item-value="value"
                required
                hint="Character separating the values"
                label="Separator *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model.number="tableImport.skip_lines"
                :rules="[
                  v => isNonNegativeInteger(v) || $t('Greater or equal to zero')]"
                type="number"
                required
                hint="Skip n lines from the top. These may include comments or the header of column names."
                label="Number of lines to skip *"
                placeholder="e.g. 0" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-select
                v-model="tableImport.quote"
                :items="quotes"
                item-text="key"
                item-value="value"
                hint="Character quoting the values"
                label="Value quotes" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.null_element"
                hint="Representation of 'no value present'"
                placeholder="e.g. NA"
                label="NULL Element" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.true_element"
                label="True Element"
                hint="Representation of boolean 'true'"
                placeholder="e.g. 1, true, YES" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.false_element"
                label="False Element"
                hint="Representation of boolean 'false'"
                placeholder="e.g. 0, false, NO" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-btn class="mr-2 mb-1" @click="step = 1">Back</v-btn>
              <v-btn
                class="mb-1"
                :disabled="!validStep2"
                :loading="loading"
                color="primary"
                type="submit"
                @click="step = 3">
                Continue
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-stepper-content>

      <v-stepper-step :complete="step > 3" step="3">
        Import Data
      </v-stepper-step>

      <v-stepper-content step="3">
        <v-form ref="form" v-model="validStep3" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="4">
              <v-file-input
                v-model="fileModel"
                accept=".csv,.tsv"
                show-size
                label="File Upload (.csv/.tsv)" />
            </v-col>
            <v-col cols="4">
              <v-text-field
                v-model="url"
                disabled
                accept=".csv,.tsv"
                show-size
                hint="e.g. http://www.wienerlinien.at/ogd_realtime/doku/ogd/wienerlinien-ogd-verbindungen.csv"
                label="File URL (.csv/.tsv)" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-btn class="mr-2 mb-1" @click="step = 2">Back</v-btn>
              <v-btn
                class="mb-1"
                :disabled="!fileModel"
                :loading="loading"
                color="primary"
                type="submit"
                @click="uploadAndAnalyse">
                Continue
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-stepper-content>

      <v-stepper-step :complete="step > 4" step="4">
        Table Schema
      </v-stepper-step>

      <v-stepper-content step="4">
        <TableSchema :back="true" :error="error" :columns="tableCreate.columns" @close="schemaClose" />
      </v-stepper-content>

      <v-stepper-step
        :complete="step > 5"
        step="5">
        Done
      </v-stepper-step>

      <v-stepper-content step="5">
        <div class="mt-2">
          <v-btn class="mb-1" color="primary" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${newTableId}`">
            View Table
          </v-btn>
        </div>
      </v-stepper-content>
    </v-stepper>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableSchema from '@/components/TableSchema'
const { notEmpty, isNonNegativeInteger } = require('@/utils')
export default {
  name: 'TableFromCSV',
  components: {
    TableSchema
  },
  data () {
    return {
      step: 1,
      validStep1: false,
      validStep2: false,
      validStep3: false,
      validStep4: false,
      error: false,
      fileModel: null,
      file: {
        filename: null,
        path: null
      },
      separators: [
        { key: ',', value: ',' },
        { key: ';', value: ';' },
        { key: '[Tab]', value: '\t' }
      ],
      quotes: [
        { key: 'Double "', value: '"' },
        { key: 'Single \'', value: '\'' }
      ],
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        { text: 'Tables', to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`, activeClass: '' }
      ],
      rules: {
        required: value => !!value || 'Required'
      },
      dateFormats: [],
      tableNames: [],
      tableCreate: {
        name: null,
        description: null,
        columns: []
      },
      tableImport: {
        location: null,
        quote: null,
        false_element: null,
        true_element: null,
        null_element: null,
        separator: ',',
        skip_lines: 1
      },
      loading: false,
      url: null,
      columns: [],
      newTableId: 42 // FIXME ???
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    fileConfig () {
      return { headers: { 'Content-Type': 'multipart/form-data' } }
    },
    sharedFilesystem () {
      return this.$config.sharedFilesystem
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
    this.loadDateFormats()
    this.listTables()
  },
  methods: {
    notEmpty,
    isNonNegativeInteger,
    uploadAndAnalyse () {
      return this.upload()
        .then(() => this.analyse())
    },
    submit () {
      this.$refs.form.validate()
    },
    async upload () {
      this.loading = true
      const data = new FormData()
      data.append('file', this.fileModel)
      try {
        const res = await this.$axios.post('/server-middleware/upload', data, this.fileConfig)
        console.debug('file upload', res.data)
        this.file = res.data
      } catch (err) {
        console.error('Failed to upload .csv data', err)
        console.debug('failed to upload .csv data, does the .csv contain a header line?')
        this.$toast.error('Could not upload data.')
      }
      this.loading = false
    },
    async analyse () {
      this.loading = true
      try {
        const payload = { filepath: `${this.sharedFilesystem}/${this.file.filename}` }
        const res = await this.$axios.post('/api/analyse/determinedt', payload, this.config)
        const { columns } = res.data
        console.log('data analyse result', columns)
        this.tableCreate.columns = Object.entries(columns)
          .map(([key, val]) => {
            return {
              name: key,
              type: val,
              check_expression: null,
              foreign_key: null,
              references: null,
              null_allowed: true,
              primary_key: false,
              unique: null,
              enum_values: []
            }
          })
        this.tableImport.location = `/tmp/${this.file.filename}`
        this.step = 4
        this.loading = false
        console.debug('upload csv', res.data)
        return
      } catch (err) {
        console.error('Failed to upload .csv data', err)
        console.debug('failed to upload .csv data, does the .csv contain a header line?')
        this.$toast.error('Could not upload data.')
      }
      this.loading = false
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
        this.step = 3
        return
      }
      this.validStep4 = true
      this.createTable()
    },
    setOthers (column) {
      column.null_allowed = false
      column.unique = true
    },
    async loadDateFormats () {
      const getUrl = `/api/container/${this.$route.params.container_id}`
      let getResult
      try {
        this.loading = true
        getResult = await this.$axios.get(getUrl, this.config)
        this.dateFormats = getResult.data.image.date_formats
        console.debug('retrieve image date formats', this.dateFormats)
        this.loading = false
      } catch (err) {
        this.loading = false
        console.error('retrieve image date formats failed', err)
      }
    },
    async createTable () {
      /* make enum values to array */
      const validColumns = this.tableCreate.columns.map((column) => {
        // validate `id` column: must be a PK
        if (column.name === 'id' && (!column.primary_key)) {
          this.$toast.error('Column `id` has to be a Primary Key')
          return false
        }
        if (column.enum_values === null) {
          return false
        }
        if (column.enum_values.length > 0) {
          column.enum_values = column.enum_values.split(',')
        }
        return true
      })

      // bail out if there is a problem with one of the columns
      if (!validColumns.every(Boolean)) { return }

      const createUrl = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table`
      let createResult
      try {
        this.loading = true
        createResult = await this.$axios.post(createUrl, this.tableCreate, this.config)
        this.newTableId = createResult.data.id
        console.debug('created table', createResult.data)
      } catch (err) {
        this.loading = false
        this.error = true
        if (err.response.status === 409) {
          this.$toast.error('Table name already exists.')
        } else {
          this.$toast.error('Could not create table.')
        }
        console.error('create table failed', err)
        return
      }
      const insertUrl = `/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${createResult.data.id}/data/import`
      let insertResult
      try {
        insertResult = await this.$axios.post(insertUrl, this.tableImport, this.config)
        console.debug('inserted table', insertResult.data)
      } catch (err) {
        this.loading = false
        this.error = true
        console.error('insert table failed', err)
        this.$toast.error('Could not insert csv into table.')
        return
      }
      this.loading = false
      this.step = 5
    }
  }
}
</script>

<style scoped>
</style>
