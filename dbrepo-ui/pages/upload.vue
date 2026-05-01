<template>
  <div>
    <v-file-input
      v-model="file"
      type="file"
      ref="file"
      @update:model-value="upload" />
  </div>
</template>
<script>

export default {
  data () {
    return {
      file: null,
    }
  },
  methods: {
    upload () {
      if (!this.file || this.file.length === 0) {
        return
      }
      console.debug('upload file', this.file)
      const uploadService = useUploadService()
      uploadService.legacy(this.file)
        .then((filename) => {
          console.debug('uploaded file', filename)
        })
        .catch((error) => {
          console.error('Failed to upload dataset', error)
          this.loading = false
        })
    }
  }
}
</script>
