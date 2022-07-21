<template>
  <div>
    <editor
      v-model="content"
      :value="value || content"
      lang="sql"
      :theme="theme"
      width="600"
      :height="height"
      @init="editorInit" />
  </div>
</template>

<script>
export default {
  components: {
    editor: require('vue2-ace-editor')
  },
  props: {
    value: {
      type: String,
      default: () => ''
    },
    disabled: {
      type: Boolean,
      default: () => false
    }
  },
  data () {
    return {
      content: this.value || '-- MariaDB 10.5 Query\n',
      theme: 'xcode'
    }
  },
  computed: {
    height () {
      return 150
      // if (!this.disabled) { return 150 }
      // const numLines = this.value.split('\n').length
      // return numLines * 25
    }
  },
  watch: {
    content (v) {
      this.$emit('input', v)
    },
    value (v) {
      this.content = v
    }
  },
  mounted () {
  },
  methods: {
    editorInit (editor) {
      editor.setOptions({
        fontSize: '12pt',
        readOnly: this.disabled,
        behavioursEnabled: !this.disabled
      })
      require('brace/ext/language_tools') // language extension prerequsite...
      require('brace/mode/html')
      require('brace/mode/sql') // language
      require('brace/mode/less')
      require('brace/theme/xcode')
      require('brace/snippets/sql') // snippet
      editor.renderer.setOptions({
        selectionStyle: 'text',
        showGutter: false
      })
      this.$emit('input', this.content)
    }
  }
}
</script>

<style scoped>
</style>
