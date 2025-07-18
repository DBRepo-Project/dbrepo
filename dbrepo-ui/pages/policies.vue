<template>
  <div
    v-if="policies">
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
import policies from '~/content/policies.md?raw'

export default {
  components: {
    Markdown
  },
  data() {
    return {
      source: policies
    }
  }
}
</script>
