<template>
  <div v-if="canInsertTableData">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        Create Table Schema (and Import Data) from .csv/.tsv
      </v-toolbar-title>
    </v-toolbar>
    <v-stepper v-model="step" vertical flat tile>
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
              <v-textarea
                v-model="tableCreate.description"
                :rules="[v => notEmpty(v) || $t('Required')]"
                autocomplete="off"
                rows="3"
                name="description"
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
                hint="max. 100 GB file size"
                persistent-hint
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
        <TableSchema :back="true" :error="error" :loading="loading" :columns="tableCreate.columns" @close="schemaClose" />
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
import { notEmpty, isNonNegativeInteger } from '@/utils'
import TableSchema from '@/components/TableSchema'
import ContainerService from '@/api/container.service'
import TableService from '@/api/table.service'
import MiddlewareService from '@/api/middleware.service'
import AnalyseService from '@/api/analyse.service'

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
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
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
    },
    canInsertTableData () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
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
    upload () {
      this.loading = true
      return new Promise((resolve, reject) => {
        MiddlewareService.upload(this.fileModel)
          .then((file) => {
            this.file = file
            resolve(file)
          })
          .catch((error) => {
            reject(error)
          })
          .finally(() => {
            this.loading = false
          })
      })
    },
    analyse () {
      this.loading = true
      AnalyseService.determineDataTypes(`/tmp/${this.file.filename}`)
        .then((analysis) => {
          const { columns } = analysis
          this.tableCreate.columns = Object.entries(columns)
            .map(([key, val]) => {
              return {
                name: key,
                type: val,
                null_allowed: true,
                primary_key: false,
                enum_values: []
              }
            })
          this.tableImport.location = `/tmp/${this.file.filename}`
          this.step = 4
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    listTables () {
      this.loading = true
      TableService.findAll(this.$route.params.container_id, this.$route.params.database_id)
        .then((tables) => {
          this.tableNames = tables.map(t => t.internal_name)
        })
        .finally(() => {
          this.loading = false
        })
    },
    schemaClose (event) {
      console.debug('schema closed', event)
      if (!event.success) {
        this.step = 3
        return
      }
      this.validStep4 = true
      this.step = 5
      this.createTable()
    },
    setOthers (column) {
      column.null_allowed = false
      column.unique = true
    },
    async loadDateFormats () {
      this.loading = true
      const res = await ContainerService.findOne(this.$route.params.container_id)
      this.dateFormats = await ContainerService.findImage(res.image.id).date_formats
      this.loading = true
    },
    createTable () {
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

      TableService.create(this.$route.params.container_id, this.$route.params.database_id, this.tableCreate)
        .then((table) => {
          this.newTableId = table.id
          TableService.importCsv(this.$route.params.container_id, this.$route.params.database_id, table.id, this.tableImport)
            .then(() => {
              this.$toast.success('Successfully created table from import!')
              this.step = 5
            })
            .finally(() => {
              this.loading = false
            })
        })
        .catch(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
</style>
