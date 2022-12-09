<template>
  <div v-if="token">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
          <v-card-title>Personal Access Tokens</v-card-title>
          <v-card-subtitle>Authentication tokens to access the HTTP API</v-card-subtitle>
          <v-card-text>
            <v-list-item v-for="(item, i) in tokens" :key="i" three-line>
              <v-list-item-content>
                <v-list-item-title :class="tokenClass(item)">sha256:{{ item.token_hash }}</v-list-item-title>
                <v-list-item-subtitle v-if="!item.token" :class="tokenClass(item)">
                  Last used: <span v-if="item.last_used">{{ format(item.last_used) }}</span><span v-if="!item.last_used">Never</span> &mdash; valid until: {{ format(item.expires) }}
                </v-list-item-subtitle>
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
                  <a @click="revokeToken(item.id)">Revoke Token</a>
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-btn :disabled="tokens.length >= tokenMax" class="mt-4" color="secondary" small @click="mintToken">
              Create Token
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
    },
    tokenMax () {
      return this.$config.tokenMax
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
    format (timestamp) {
      return formatTimestamp(timestamp)
    },
    tokenClass (token) {
      return token.last_used ? '' : 'token-not_used'
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
    async revokeToken (id) {
      this.loading = true
      try {
        await this.$axios.delete(`/api/user/token/${id}`, this.config)
        await this.loadTokens()
      } catch (err) {
        this.$toast.error('Could not delete token')
      }
      this.loading = false
    }
  }
}
</script>
<style>
.token-not_used {
  opacity: 0.4;
}
</style>
