<template>
  <div v-if="canInsertTableData">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/database/${$route.params.database_id}/table`">
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
              <v-text-field
                v-model="tableCreate.description"
                autocomplete="off"
                name="description"
                label="Description (short)" />
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
            <v-col cols="8">
              <v-alert
                v-if="warnAnalyseSeparator"
                border="left"
                color="warning">
                We analysed your .csv/.tsv file and found that the separator you provided
                <code>{{ tableImport.separator }}</code> is not correct, the separator
                <code>{{ suggestedAnalyseSeparator }}</code> is more likely to be correct. If you really want to import
                the .csv/.tsv file still, click "continue".
              </v-alert>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-file-input
                v-model="fileModel"
                accept=".csv,.tsv"
                hint="max. 2 GB file size"
                persistent-hint
                :show-size="1000"
                counter
                label="File Upload (.csv/.tsv)" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="6">
              <v-btn class="mr-2 mb-1" @click="step = 2">Back</v-btn>
              <v-btn
                class="mb-1"
                :disabled="!fileModel"
                :loading="loadingUpload || loadingAnalyse"
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
        <TableSchema :back="true" :error="error" :loading="loadingImage" :columns="tableCreate.columns" @close="schemaClose" />
      </v-stepper-content>
      <v-stepper-step
        :complete="step > 5"
        step="5">
        Done
      </v-stepper-step>
      <v-stepper-content step="5">
        <div class="mt-2">
          <v-btn class="mb-1" color="primary" :to="`/database/${$route.params.database_id}/table/${newTableId}`">
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
import TableSchema from '@/components/TableSchema.vue'
import TableService from '@/api/table.service'
import AnalyseService from '@/api/analyse.service'
import DatabaseService from '@/api/database.service'
import QueryMapper from '@/api/query.mapper'
import TableMapper from '@/api/table.mapper'
import UploadService from '@/api/upload.service'

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
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        },
        { text: 'Tables', to: `/database/${this.$route.params.database_id}/table`, activeClass: '' }
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
        quote: '"',
        false_element: null,
        true_element: null,
        null_element: null,
        separator: ',',
        skip_lines: 1
      },
      loading: false,
      loadingUpload: false,
      loadingAnalyse: false,
      loadingImage: false,
      warnAnalyseSeparator: false,
      suggestedAnalyseSeparator: null,
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
        .then(path => this.analyse(path))
    },
    submit () {
      this.$refs.form.validate()
    },
    upload () {
      this.loadingUpload = true
      return new Promise((resolve, reject) => {
        UploadService.upload(this.fileModel)
          .then((file) => {
            console.debug('uploaded file', file)
            resolve(file.path)
          })
          .catch((error) => {
            this.loadingUpload = false
            reject(error)
          })
          .finally(() => {
            this.loadingUpload = false
          })
      })
    },
    analyse (path) {
      this.loadingAnalyse = true
      AnalyseService.determineDataTypes(path)
        .then((analysis) => {
          const { columns, separator } = analysis
          const dataTypes = QueryMapper.mySql8DataTypes()
          this.tableCreate.columns = Object.entries(columns)
            .map(([key, val]) => {
              return {
                name: key,
                type: val,
                null_allowed: true,
                primary_key: false,
                size: dataTypes.filter(d => d.value === val).length > 0 ? dataTypes.filter(d => d.value === val)[0].defaultSize : null,
                d: dataTypes.filter(d => d.value === val).length > 0 ? dataTypes.filter(d => d.value === val)[0].defaultD : null,
                enums: [],
                sets: []
              }
            })
          this.tableImport.location = path
          if (separator !== this.tableImport.separator) {
            this.warnAnalyseSeparator = true
            this.suggestedAnalyseSeparator = separator
          } else {
            this.step = 4
          }
        })
        .finally(() => {
          this.loadingAnalyse = false
        })
    },
    listTables () {
      this.loading = true
      TableService.findAll(this.$route.params.database_id)
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
      this.createTable()
    },
    async loadDateFormats () {
      this.loadingImage = true
      try {
        const database = await DatabaseService.findOne(this.$route.params.database_id)
        this.dateFormats = database.container.image.date_formats
      } finally {
        this.localLoading = false
      }
    },
    createTable () {
      /* make enum values to array */
      const validColumns = this.tableCreate.columns.map((column) => {
        // validate `id` column: must be a PK
        if (column.name === 'id' && (!column.primary_key)) {
          this.$toast.error('Column `id` has to be a Primary Key')
          return false
        }
        return true
      })
      // bail out if there is a problem with one of the columns
      if (!validColumns.every(Boolean)) { return }
      const table = TableMapper.tableCreateToTableCreateDto(this.tableCreate)
      TableService.create(this.$route.params.database_id, table)
        .then((table) => {
          this.newTableId = table.id
          TableService.importCsv(this.$route.params.database_id, table.id, this.tableImport)
            .then(async () => {
              this.$toast.success('Successfully created table from import!')
              await this.$store.dispatch('reloadDatabase')
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
