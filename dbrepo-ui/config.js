const config = {}

config.title = process.env.TITLE || null
config.icon = process.env.ICON || null
config.brokerUsername = process.env.BROKER_USERNAME || null
config.brokerPassword = process.env.BROKER_PASSWORD || null
config.brokerLoginUrl = process.env.BROKER_LOGIN_URL || null
config.keycloakLoginUrl = process.env.KEYCLOAK_LOGIN_URL || null
config.sharedFilesystem = process.env.SHARED_FILESYSTEM || null
config.version = process.env.VERSION || null
config.logo = process.env.LOGO || null
config.tokenMax = process.env.TOKEN_MAX || null
config.searchUsername = process.env.SEARCH_USERNAME || null
config.searchPassword = process.env.SEARCH_PASSWORD || null
config.clientId = process.env.DBREPO_CLIENT_ID || null
config.clientSecret = process.env.DBREPO_CLIENT_SECRET || null
config.defaultPublisher = process.env.DEFAULT_PID_PUBLISHER || null
config.doiUrl = process.env.DOI_URL || null
config.uploadPath = process.env.UPLOAD_PATH || null

module.exports = config
