<template>
  <div
    v-if="source">
    <v-card
      rounded="0"
      variant="flat">
      <v-card-text>
        <vue-markdown-it
          :source="source" />
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import { VueMarkdownIt } from '@f3ve/vue-markdown-it'

export default {
  components: {
    VueMarkdownIt
  },
  data() {
    return {
      source: this.$config.public.terms.content
    }
  }
}
</script>
