const config = {}

config.title = process.env.TITLE || 'Database Repository'
config.icon = process.env.ICON || '/favicon.ico'
config.brokerUsername = process.env.BROKER_USERNAME || 'fda'
config.brokerPassword = process.env.BROKER_PASSWORD || 'fda'
config.sharedFilesystem = process.env.SHARED_FILESYSTEM || '/tmp'
config.version = process.env.VERSION || 'latest'
config.logo = process.env.LOGO || '/logo.png'
config.tokenMax = process.env.TOKEN_MAX || 5
config.searchUsername = process.env.SEARCH_USERNAME || 'admin'
config.searchPassword = process.env.SEARCH_PASSWORD || 'admin'
config.clientId = process.env.DBREPO_CLIENT_ID || 'dbrepo-client'
config.clientSecret = process.env.DBREPO_CLIENT_SECRET || 'MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG'
config.defaultPublisher = process.env.DEFAULT_PID_PUBLISHER || 'Example University'
config.doiUrl = process.env.DOI_URL || 'https://doi.org'
config.uploadPath = process.env.UPLOAD_PATH || '/tmp/'
config.forceSsl = process.env.FORCE_SSL || 'false'

module.exports = config
