<template>
  <div v-if="canInsertTableData">
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/database/${$route.params.database_id}/table`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title>
        {{ table.name }}
      </v-toolbar-title>
    </v-toolbar>
    <v-stepper v-model="step" vertical flat tile>
      <v-stepper-step :complete="step > 1" step="1">
        Import Data
      </v-stepper-step>
      <v-stepper-content step="1">
        <v-form ref="form" v-model="validStep1" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="8">
              <v-select
                v-model="tableImport.separator"
                :items="separators"
                item-text="key"
                item-value="value"
                required
                clearable
                hint="Character separating the values"
                label="Separator *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model.number="tableImport.skip_lines"
                :rules="[v => isNonNegativeInteger(v) || $t('Greater or equal to zero')]"
                type="number"
                required
                clearable
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
                clearable
                hint="Character quoting the values"
                label="Value quotes" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.line_termination"
                hint="Representation of a new line"
                placeholder="e.g. \r\n"
                clearable
                label="Line termination" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.null_element"
                hint="Representation of 'no value present'"
                placeholder="e.g. NA"
                clearable
                label="NULL Element" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.true_element"
                label="True Element"
                clearable
                hint="Representation of boolean 'true'"
                placeholder="e.g. 1, true, YES" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-text-field
                v-model="tableImport.false_element"
                label="False Element"
                clearable
                hint="Representation of boolean 'false'"
                placeholder="e.g. 0, false, NO" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="8">
              <v-file-input
                v-model="fileModel"
                accept=".csv,.tsv"
                hint="max. 2GB file size"
                persistent-hint
                clearable
                :show-size="1000"
                counter
                label="CSV/TSV File" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="8">
              <v-btn :disabled="!fileModel" :loading="loading" color="primary" @click="uploadAndImport">Import</v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-stepper-content>
    </v-stepper>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import TableService from '@/api/table.service'
import QueryService from '@/api/query.service'
import UploadService from '@/api/upload.service'
const { isNonNegativeInteger } = require('@/utils')

export default {
  name: 'TableImportCSV',
  components: {
  },
  data () {
    return {
      loading: false,
      step: 1,
      ready: false,
      validStep1: false,
      separators: [
        { key: ',', value: ',' },
        { key: ';', value: ';' },
        { key: '[Tab]', value: '\t' }
      ],
      quotes: [
        { key: 'Double "', value: '"' },
        { key: 'Single \'', value: '\'' }
      ],
      table: {
        id: null,
        name: null,
        internal_name: null
      },
      tableImport: {
        location: null,
        quote: '"',
        false_element: null,
        true_element: null,
        null_element: '',
        separator: ',',
        line_termination: '\\r\\n',
        skip_lines: 1
      },
      file: {
        filename: null,
        path: null
      },
      fileModel: null,
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
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
    canInsertTableData () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('insert-table-data')
    }
  },
  mounted () {
    this.loadTableMetadata()
  },
  methods: {
    isNonNegativeInteger,
    uploadAndImport () {
      this.loading = true
      UploadService.upload(this.$config.uploadEndpointUrl, this.fileModel)
        .then((metadata) => {
          console.debug('uploaded file', metadata)
          const { s3key } = metadata
          this.tableImport.location = s3key
          QueryService.importCsv(this.$route.params.database_id, this.$route.params.table_id, this.tableImport)
            .then((metadata) => {
              console.debug('successfully imported data', metadata)
              this.$toast.success('Successfully imported data')
              this.$router.push(`/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`)
            })
            .catch((error) => {
              this.$toast.error('Failed to import data', error)
              this.loading = false
            })
            .finally(() => {
              this.loading = false
            })
        })
    },
    submit () {
      this.$refs.form.validate()
    },
    loadTableMetadata () {
      this.loading = true
      TableService.findOne(this.$route.params.database_id, this.$route.params.table_id)
        .then((table) => {
          this.table = table
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
<style>
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
