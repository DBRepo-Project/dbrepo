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
      uploadService.create(this.file[0])
        .then((filename) => {
          console.debug('uploaded file', filename)
          this.filename = filename
          this.value = filename
          this.$emit('blob', { column: this.column, s3key: filename })
        })
        .catch((error) => {
          this.$toast.error(this.$t(error.code))
        })
    }
  }
}
</script>
