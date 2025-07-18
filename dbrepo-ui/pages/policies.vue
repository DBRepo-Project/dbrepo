<template>
  <div>
    <v-card
      rounded="0"
      variant="flat">
      <v-card-text>
        <Markdown
          :source="source" />
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import Markdown from 'vue3-markdown-it'
import source from '~/content/about.md?raw'

export default {
  components: {
    Markdown
  },
  data() {
    return {
      source: source
    }
  }
}
</script>
