<template>
  <div>
    <editor
      v-model="content"
      :class="{ 'theme-dark': is_dark }"
      :value="value || content"
      lang="sql"
      theme="xcode"
      width="100%"
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
      content: this.value
    }
  },
  computed: {
    is_dark () {
      return this.$vuetify.theme.dark
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
        behavioursEnabled: !this.disabled,
        maxLines: 28,
        minLines: 16
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
.theme-dark {
  background: none;
  color: white;
}
</style>
