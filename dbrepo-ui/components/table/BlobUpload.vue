<template>
  <div v-if="column">
    <v-file-input
      ref="blob"
      v-model="file"
      :label="column.internal_name"
      type="file"
      @update:model-value="upload" />
  </div>
</template>
<script>
export default {
  props: {
    column: {
      type: Object,
      default: null
    }
  },
  data () {
    return {
      file: null,
      value: null,
      filename: null
    }
  },
  computed: {
  },
  methods: {
    upload () {
      if (!this.file || this.file.length === 0) {
        return
      }
      const uploadService = useUploadService()
      uploadService.upload(this.file[0])
        .then((metadata) => {
          console.debug('uploaded file', metadata)
          const { s3key } = metadata
          this.filename = metadata.file.name
          this.value = s3key
          this.$emit('blob', { column: this.column, s3key: this.value })
        })
        .catch((error) => {
          console.error(`Failed to set column value: ${this.column.internal_name}`, error)
          this.$toast.error(this.$t('error.data.value') + ' ' + this.column.internal_name)
        })
    }
  }
}
</script>
