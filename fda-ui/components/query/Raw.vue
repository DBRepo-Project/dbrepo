<template>
  <div>
    <editor
      v-model="content"
      lang="sql"
      theme="chrome"
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
      content: 'SELECT `id` FROM "myTable"'
    }
  },
  computed: {
  },
  mounted () {
  },
  methods: {
    editorInit (editor) {
      editor.setOptions({
        // fontFamily: 'tahoma',
        fontSize: '12pt'
      })
      require('brace/ext/language_tools') // language extension prerequsite...
      require('brace/mode/html')
      require('brace/mode/sql') // language
      require('brace/mode/less')
      require('brace/theme/chrome')
      require('brace/snippets/sql') // snippet
    }
  }
}
</script>

<style scoped>
</style>
