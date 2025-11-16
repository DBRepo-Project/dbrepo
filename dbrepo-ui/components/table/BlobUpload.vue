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
  methods: {
    upload () {
      if (!this.file || this.file.length === 0) {
        return
      }
      console.debug('upload file', this.file)
      const uploadService = useUploadService()
      uploadService.create(this.file)
        .then((s3_key) => {
          this.filename = s3_key
          this.value = s3_key
          this.$emit('blob', { column: this.column, s3key: s3_key })
        })
        .catch((error) => {
          console.error('Failed to upload dataset', error)
          const toast = useToastInstance()
          toast.error(this.$t('error.upload.dataset'))
          this.loading = false
        })
    }
  }
}
</script>
