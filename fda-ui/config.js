const config = {}

config.api = process.env.API || 'http://localhost:9095'
config.search = process.env.SEARCH || 'http://localhost:9200'
config.sandbox = process.env.SANDBOX || false
config.title = process.env.TITLE || 'Database Repository'
config.icon = process.env.ICON || '/favicon.ico'
config.brokerUsername = process.env.BROKER_USERNAME || 'fda'
config.brokerPassword = process.env.BROKER_PASSWORD || 'fda'
config.sharedFilesystem = process.env.SHARED_FILESYSTEM || '/tmp'
config.version = process.env.VERSION || 'latest'
config.logo = process.env.LOGO || '/logo.png'
config.mailVerify = process.env.MAIL_VERIFY || false
config.tokenMax = process.env.TOKEN_MAX || 5
config.elasticPassword = process.env.ELASTIC_PASSWORD || 'elastic'
config.elasticPassword = process.env.ELASTIC_PASSWORD || 'elastic'
config.clientSecret = process.env.CLIENT_SECRET

module.exports = config
