<template>
  <v-btn
    :loading="loading"
    v-bind="$attrs"
    @click.stop="download">
    <slot />
  </v-btn>
</template>

<script>
import IdentifierService from '@/api/identifier.service'

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
      IdentifierService.export(this.pid)
        .then((data) => {
          const url = window.URL.createObjectURL(new Blob([data]))
          const link = document.createElement('a')
          link.href = url
          link.setAttribute('download', this.filename)
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
