<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>Import Data</v-toolbar-title>
      <v-spacer />
      <v-toolbar-title>
        <v-btn :to="`/container/${$route.params.container_id}/database/${$route.params.database_id}/table/${$route.params.table_id}`">
          <v-icon left>mdi-table</v-icon>
          View Table
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-card>
      <v-card-title v-if="!loading">
        {{ table.name }}
      </v-card-title>
      <v-card-subtitle>{{ table.internal_name }}</v-card-subtitle>
      <v-card-text>
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
              :rules="[v => isNonNegativeInteger(v) || $t('Greater or equal to zero')]"
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
          <v-col cols="8">
            <v-file-input
              v-model="file"
              accept=".csv,.tsv"
              show-size
              label="CSV/TSV File" />
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions>
        <v-btn :disabled="!file" :loading="loading" color="primary" @click="upload">Upload</v-btn>
      </v-card-actions>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
const { isNonNegativeInteger } = require('@/utils')
export default {
  name: 'TableImportCSV',
  components: {
  },
  data () {
    return {
      loading: false,
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
        quote: null,
        false_element: null,
        true_element: null,
        null_element: null,
        separator: ',',
        skip_lines: 1
      },
      file: null,
      fileLocation: null,
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tableId () {
      return this.$route.params.table_id
    },
    databaseId () {
      return this.$route.params.database_id
    },
    token () {
      return this.$store.state.token
    }
  },
  mounted () {
    this.info()
  },
  methods: {
    isNonNegativeInteger,
    async info () {
      this.loading = true
      const infoUrl = `/api/container/${this.$route.params.container_id}/database/${this.databaseId}/table/${this.tableId}`
      try {
        const res = await this.$axios.get(infoUrl)
        console.debug('got table', res.data)
        this.table = res.data
      } catch (err) {
        console.error('Could not insert data.', err)
      }
      this.loading = false
    },
    async upload () {
      this.loading = true
      const url = '/server-middleware/table_from_csv'
      const data = new FormData()
      data.append('file', this.file)
      try {
        const res = await this.$axios.post(url, data, {
          headers: {
            'Content-Type': 'multipart/form-data',
            Authorization: `Bearer ${this.token}`
          }
        })
        if (res.data.success) {
          this.fileLocation = res.data.file.filename
          this.tableImport.location = `/tmp/${this.fileLocation}`
          console.debug('upload csv', res.data)
        } else {
          console.error('Could not upload CSV data', res.data)
          return
        }
      } catch (err) {
        console.error('Could not upload data.', err)
        return
      }
      const insertUrl = `/api/container/${this.$route.params.container_id}/database/${this.databaseId}/table/${this.tableId}/data/import`
      let insertResult
      try {
        insertResult = await this.$axios.post(insertUrl, this.tableImport, {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        console.debug('inserted table', insertResult.data)
      } catch (err) {
        console.error('Could not insert data.', err)
        this.loading = false
        return
      }
      this.$toast.success('Uploaded csv into table.')
      this.loading = false
      this.$router.push(`/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}`)
    }
  }
}
</script>

<style>
</style>
