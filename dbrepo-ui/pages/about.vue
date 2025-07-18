<template>
  <div
    v-if="about">
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
import about from '~/content/about.md?raw'

export default {
  components: {
    Markdown
  },
  data() {
    return {
      source: about
    }
  }
}
</script>
