<template>
  <v-btn
    :loading="loading"
    v-bind="$attrs"
    @click.stop="download">
    <slot />
  </v-btn>
</template>

<script>
export default {
  props: {
    pid: {
      type: Number,
      default () {
        return null
      }
    },
    contentType: {
      type: String,
      default () {
        return 'text/xml'
      }
    },
    filename: {
      type: String,
      default () {
        return 'identifier.xml'
      }
    }
  },
  data () {
    return {
      loading: false
    }
  },
  methods: {
    download () {
      this.loading = true
      const identifierService = useIdentifierService()
      identifierService.findOne(this.pid, this.contentType)
        .then((data) => {
          const url = URL.createObjectURL(data)
          const link = document.createElement('a')
          link.href = url
          link.download = this.filename
          document.body.appendChild(link)
          link.click()
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
<style scoped>
</style>
