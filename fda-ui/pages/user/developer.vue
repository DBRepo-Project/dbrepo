<template>
  <div>
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
          <v-card-title>Personal Access Tokens</v-card-title>
          <v-card-subtitle>Authentication tokens to access the HTTP API</v-card-subtitle>
          <v-card-text>
            <v-list-item v-for="(item, i) in tokens" :key="i" three-line>
              <v-list-item-content>
                <v-list-item-title>sha256:{{ item.token_hash }}</v-list-item-title>
                <v-list-item-subtitle v-if="!item.token">Created on {{ formatCreationTimestamp(item.created) }}, valid until: indeterminate</v-list-item-subtitle>
                <v-list-item-subtitle v-if="item.token">
                  <v-text-field
                    v-model="item.token"
                    :append-outer-icon="item.copied ? 'mdi-check' : 'mdi-content-copy'"
                    readonly
                    hint="Copy this token, it will not be visible again!"
                    persistent-hint
                    type="text"
                    @click:append-outer="copy(item)" />
                </v-list-item-subtitle>
                <v-list-item-subtitle v-if="!item.token">
                  <a @click="revokeToken(item.token_hash)">Revoke Token</a>
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-btn class="mt-4" x-small @click="mintToken">
              Mint Token
            </v-btn>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import { formatTimestamp } from '@/utils'
export default {
  data () {
    return {
      tab: 0,
      error: false,
      tokens: [],
      loading: false
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    }
  },
  mounted () {
    this.loadTokens()
  },
  methods: {
    submit () {
    },
    copy (item) {
      item.copied = true
      navigator.clipboard.writeText(item.token)
    },
    formatCreationTimestamp (timestamp) {
      return formatTimestamp(timestamp)
    },
    async loadTokens () {
      this.loading = true
      try {
        const res = await this.$axios.get('/api/user/token', this.config)
        this.tokens = res.data.filter(t => !t.deleted)
        console.debug('tokens', this.tokens)
      } catch (err) {
        this.$toast.error('Could not load tokens')
      }
      this.loading = false
    },
    async mintToken () {
      this.loading = true
      try {
        const res = await this.$axios.post('/api/user/token', {}, this.config)
        const token = res.data
        token.copied = false
        console.debug('token', token)
        this.tokens.push(token)
      } catch (err) {
        if (err.response.status === 417) {
          this.$toast.error('Already exceeded the maximum allowed number of tokens!')
        } else {
          this.$toast.error('Could not create token')
        }
      }
      this.loading = false
    },
    async revokeToken (hash) {
      this.loading = true
      try {
        await this.$axios.delete(`/api/user/token/${hash}`, this.config)
        await this.loadTokens()
      } catch (err) {
        this.$toast.error('Could not delete token')
      }
      this.loading = false
    }
  }
}
</script>
