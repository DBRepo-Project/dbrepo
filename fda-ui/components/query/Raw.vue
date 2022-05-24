<template>
  <div>
    <editor
      v-model="content"
      lang="sql"
      :theme="theme"
      width="600"
      height="150"
      @init="editorInit" />
  </div>
</template>

<script>
export default {
  components: {
    editor: require('vue2-ace-editor')
  },
  data () {
    return {
      content: 'SELECT `id` FROM "myTable"',
      theme: 'xcode'
    }
  },
  computed: {
  },
  watch: {
    content (v) {
      this.$emit('input', v)
    }
  },
  mounted () {
  },
  methods: {
    editorInit (editor) {
      editor.setOptions({
        fontSize: '11pt'
      })
      require('brace/ext/language_tools') // language extension prerequsite...
      require('brace/mode/html')
      require('brace/mode/sql') // language
      require('brace/mode/less')
      require('brace/theme/xcode')
      require('brace/snippets/sql') // snippet
      editor.renderer.setOptions({
        showGutter: false
      })
      this.$emit('input', this.content)
    }
  }
}
</script>

<style scoped>
</style>
