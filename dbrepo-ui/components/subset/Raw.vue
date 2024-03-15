<template>
  <div>
    <VAceEditor
      ref="aceRef"
      v-model:value="content"
      class="vue-ace-editor"
      lang="sql"
      theme="chrome"
      :options="options"
      width="100%" />
  </div>
</template>

<script>
import { VAceEditor } from 'vue3-ace-editor'
import 'ace-builds/src-noconflict/theme-chrome'
import 'ace-builds/src-noconflict/mode-sql'

export default {
  components: {
    VAceEditor
  },
  props: {
    value: {
      type: String,
      default: () => null
    },
    disabled: {
      type: Boolean,
      default: () => false
    }
  },
  data () {
    return {
      content: this.value,
      options: {
        enableSnippets: true,
        readOnly: this.disabled,
        maxLines: 100,
        fontSize: '10pt'
      }
    }
  },
  watch: {
    value: {
      handler () {
        this.content = this.value
      },
      immediate: true
    },
    content: {
      handler () {
        this.$emit('sql', { raw: this.content })
      },
      immediate: true
    }
  },
}
</script>
<style lang="scss" scoped>
</style>
